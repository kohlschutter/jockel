/*
 * jockel
 *
 * Copyright 2024 Christian Kohlschütter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kohlschutter.jockel.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.dumbo.RenderState;
import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.jockel.core.liqp.AssetPathTag;
import com.kohlschutter.jockel.core.liqp.DateToXmlschemaFilter;
import com.kohlschutter.jockel.core.liqp.JockelIncludeTag;
import com.kohlschutter.jockel.core.liqp.JsonifyFilter;
import com.kohlschutter.jockel.core.liqp.MarkdownifyFilter;
import com.kohlschutter.jockel.core.liqp.NumberOfWordsFilter;
import com.kohlschutter.jockel.core.liqp.SeoTag;
import com.kohlschutter.jockel.core.liqp.SlugifyFilter;
import com.kohlschutter.jockel.core.site.CustomSiteVariables;
import com.kohlschutter.jockel.core.site.PermalinkParser;
import com.kohlschutter.jockel.core.util.PathReaderSupplier;
import com.kohlschutter.stringhold.CachedIOSupplier;
import com.kohlschutter.stringhold.HasExpectedLength;
import com.kohlschutter.stringhold.HasLength;
import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;
import com.kohlschutter.stringhold.StringHolder;
import com.kohlschutter.stringhold.liqp.Conditional;
import com.kohlschutter.stringhold.liqp.Conditionally;
import com.kohlschutter.stringhold.liqp.StringHolderRenderTransformer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.sequence.LineAppendable;

import liqp.Template;
import liqp.TemplateParser;
import liqp.TemplateParser.ErrorMode;
import liqp.TemplateParser.StrictVariablesMode;
import liqp.parser.Flavor;

@SuppressWarnings("PMD.ExcessiveImports")
public class LiquidHelper {
  private static final Logger LOG = LoggerFactory.getLogger(LiquidHelper.class);

  private static final char[] FRONT_MATTER_LINE = {'-', '-', '-', '\n'};

  public static final String ENVIRONMENT_KEY_DUMBO_APP = ".dumbo.app";

  // snakeyaml
  private final LoadSettings loadSettings = YAMLSupport.DEFAULT_LOAD_SETTINGS;

  private final TemplateParser liqpParser;

  private final ServerApp app;

  private final Map<String, Object> commonVariables;

  private Function<Object, Object> contentTransformer = (o) -> o;

  // FIXME revisit this
  static final class GraalVMStub {
    static final MarkdownServlet OBJ1 = new MarkdownServlet();
    static final HtmlJspFilter OBJ2 = new HtmlJspFilter();
    static final HtmlRenderer OBJ3 = HtmlRenderer.builder().build();
    static final LineAppendable.Options[] OBJ4 = LineAppendable.Options.values();
  }

  LiquidHelper(ServerApp app, Map<String, Object> commonVariables) {
    this.app = app;
    this.commonVariables = commonVariables;
    this.liqpParser = newLiqpParser(app);
  }

  void setContentTransformer(Function<Object, Object> transformer) {
    this.contentTransformer = transformer;
  }

  private boolean hasFrontMatter(Reader in) throws IOException {
    boolean haveFrontMatter;
    char[] cbuf = new char[4];
    in.mark(cbuf.length);

    haveFrontMatter = (in.read(cbuf) == cbuf.length && Arrays.equals(cbuf, FRONT_MATTER_LINE));
    in.reset();
    return haveFrontMatter;
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, Map<String, Object> variablesIn,
      Supplier<Map<String, Object>> pageVariablesSupplier) throws IOException {
    return prerenderLiquid(inSup, variablesIn, LiquidVariables.PAGE, pageVariablesSupplier);
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, Map<String, Object> variablesIn,
      String collectionItemType, Supplier<Map<String, Object>> itemVariablesSupplier)
      throws IOException {
    return prerenderLiquid(inSup, 0, variablesIn, collectionItemType, itemVariablesSupplier);
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, int expectedLen,
      Map<String, Object> variablesIn) throws IOException {
    return prerenderLiquid(inSup, expectedLen, variablesIn, () -> new HashMap<>());
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, int expectedLen,
      Map<String, Object> variables, Supplier<Map<String, Object>> pageVariablesSupplier)
      throws IOException {
    return prerenderLiquid(inSup, expectedLen, variables, LiquidVariables.PAGE,
        pageVariablesSupplier);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> parseFrontMatter(PathReaderSupplier inSup,
      Map<String, Object> variables, String collectionItemType,
      Supplier<Map<String, Object>> pageVariablesSupplier) throws IOException {
    return (Map<String, Object>) prerenderLiquid(inSup, 0, variables, collectionItemType,
        pageVariablesSupplier, true);
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, int expectedLen,
      Map<String, Object> variables, String collectionItemType,
      Supplier<Map<String, Object>> pageVariablesSupplier) throws IOException {
    return prerenderLiquid(inSup, expectedLen, variables, collectionItemType, pageVariablesSupplier,
        false);
  }

  @SuppressWarnings("PMD.CognitiveComplexity")
  @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
  private Object prerenderLiquid(PathReaderSupplier inSup, int expectedLen,
      Map<String, Object> variables, String collectionItemType,
      Supplier<Map<String, Object>> pageVariablesSupplier, boolean justParseFrontMatter)
      throws IOException {

    Reader in = inSup.get();

    if (in instanceof HasLength) {
      expectedLen = ((HasLength) in).length();
    } else if (in instanceof HasExpectedLength) {
      expectedLen = ((HasExpectedLength) in).getExpectedLength();
    }

    if (!in.markSupported()) {
      in = new BufferedReader(in);
    }

    if (hasFrontMatter(in)) {
      // front matter enables Liquid templates

      Map<String, Object> pageVariables = pageVariablesSupplier.get();
      if (!justParseFrontMatter || variables != null) {
        Objects.requireNonNull(variables);
        variables.put(collectionItemType, pageVariables);
      }

      initDefaults(inSup.getType(), inSup.getRelativePath(), pageVariables);

      parseFrontMatter(in, pageVariables);

      pageVariables.put(LiquidVariables.PAGE_COLLECTION, inSup.getType());

      Object tags = pageVariables.get(LiquidVariables.PAGE_TAGS);
      if (tags instanceof String) {
        tags = new LinkedHashSet<>(Arrays.asList(((String) tags).split("[\\s]+")));
        pageVariables.put(LiquidVariables.PAGE_TAGS, tags);
      }

      Object categories = pageVariables.get(LiquidVariables.PAGE_CATEGORIES);
      if (categories instanceof String) {
        categories = new LinkedHashSet<>(Arrays.asList(((String) categories).split("[\\s]+")));
        pageVariables.put(LiquidVariables.PAGE_CATEGORIES, categories);
      }

      // pageVariables.put(LiquidVariables.PAGE_LAST_MODIFIED_AT, "1970-12-12 23:34:45 CET");
      pageVariables.put(LiquidVariables.PAGE_LAST_MODIFIED_AT, null);

      String url;
      try {
        CustomSiteVariables.storePathAndFilename(inSup.getRelativePath(), pageVariables);
        if (variables != null) {
          CustomSiteVariables.copyPathAndFileName(pageVariables, variables);
        }

        url = PermalinkParser.parsePermalink((String) pageVariables.get(
            LiquidVariables.PAGE_PERMALINK), pageVariables);
        pageVariables.put(LiquidVariables.PAGE_URL, url);
      } catch (ParseException e) {
        e.printStackTrace();
      }

      Template template = liqpParser.parse(in);
      for (Exception exc : template.errors()) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("Template error: {}", exc.toString());
        }
        // exc.printStackTrace();
      }

      if (justParseFrontMatter) {
        return pageVariables;
      } else {
        Object obj = template.renderToObject(variables);
        pageVariables.put(LiquidVariables.PAGE_CONTENT, StringHolder.withSupplier(
            () -> contentTransformer.apply(obj)));

        return obj;
      }
    } else {
      if (justParseFrontMatter) {
        return null;
      }
      CompletableFuture<IOException> excHolder = new CompletableFuture<IOException>();
      StringHolder s = StringHolder.withReaderSupplierExpectedLength(expectedLen,
          new CachedIOSupplier<>(in, inSup), (IOException e) -> {
            excHolder.complete(e);
            return ExceptionResponse.EXCEPTION_MESSAGE;
          });
      IOException exception = excHolder.getNow(null);
      if (exception != null) {
        throw exception;
      }
      return s;
    }
  }

  @SuppressWarnings("PMD.CognitiveComplexity")
  private void initDefaults(String type, String relativePath, Map<String, Object> pageVariables) {
    @SuppressWarnings("unchecked")
    List<Map<String, Map<String, Object>>> cv =
        (List<Map<String, Map<String, Object>>>) ((Map<String, Object>) commonVariables.get("site"))
            .get("defaults");
    if (cv == null || cv.isEmpty()) {
      return;
    }

    for (Map<String, Map<String, Object>> en : cv) {
      Map<String, Object> scope = en.get("scope");

      Object scopeType = scope.get("type");
      if (scopeType != null) {
        if (!scopeType.equals(type) && !String.valueOf(scopeType).isEmpty()) {
          continue;
        }
      }
      Object scopePath = scope.get("path");
      if (scopePath != null) {
        String scopePathStr = String.valueOf(scopePath);
        if (!scopePathStr.isEmpty()) {
          if (scopePathStr.contains("*")) {
            throw new UnsupportedOperationException("globbing is not yet supported");
          }
          if (!scopePathStr.equals(relativePath) && !(relativePath.startsWith(scopePathStr)
              && relativePath.length() > scopePathStr.length() && relativePath.charAt(scopePathStr
                  .length()) == '/')) {
            continue;
          }
        }
      }

      Map<String, Object> values = en.get("values");
      pageVariables.putAll(values);
    }
  }

  @SuppressWarnings("unchecked")
  private void parseFrontMatter(Reader in, Map<String, Object> page) throws IOException {
    BufferedReader br = (in instanceof BufferedReader) ? (BufferedReader) in : new BufferedReader(
        in);

    StringBuilder sbFrontMatter = new StringBuilder();
    sbFrontMatter.append(br.readLine());
    sbFrontMatter.append('\n');
    String l;
    do {
      l = br.readLine();
      if (l == null) {
        break;
      }
      sbFrontMatter.append(l);
      sbFrontMatter.append('\n');
    } while (!"---".equals(l));

    Iterable<Object> loadAllFromString = new Load(loadSettings).loadAllFromString(sbFrontMatter
        .toString());
    for (Object o : loadAllFromString) {
      if (o == null) {
        continue;
      } else if (o instanceof Map) {
        Map<String, Object> map = (Map<String, Object>) o;
        page.putAll(map);
      } else {
        if (LOG.isWarnEnabled()) {
          LOG.warn("Unexpected YAML object class in front matter: " + o.getClass());
        }
      }
    }
  }

  public BufferedReader layoutReader(String layout) throws IOException {
    if (layout == null || layout.isBlank() || app == null) {
      return null;
    }
    URL layoutURL = app.getResource("markdown/_layouts/" + layout + ".html");
    if (layoutURL == null) {
      return null;
    } else {
      return new BufferedReader(new InputStreamReader(layoutURL.openStream(),
          StandardCharsets.UTF_8));
    }
  }

  public Object renderLayout(String layoutId, BufferedReader in, Object contentSupply,
      Map<String, Object> variables) throws IOException {
    @SuppressWarnings("unchecked")
    Set<String> includedLayouts = (((ThreadLocal<RenderState>) ((Map<String, Object>) variables.get(
        LiquidVariables.DUMBO)).get(LiquidVariables.DUMBO_STATE_TL)).get()).getIncluded();
    do { // NOPMD.WhileLoopWithLiteralBoolean
      if (!includedLayouts.add(layoutId)) {
        IOException e = new IOException("Circular reference detected: Layout " + layoutId
            + " already detected: " + includedLayouts);
        e.printStackTrace();
        throw e;
      }

      variables.put("content", contentSupply);

      @SuppressWarnings("unchecked")
      Map<String, Object> layoutVariables = (Map<String, Object>) variables.computeIfAbsent(
          "layout", (k) -> new HashMap<>());
      layoutVariables.remove("layout");

      if (in != null) {
        if (hasFrontMatter(in)) {
          // enables Liquid templates
          parseFrontMatter(in, layoutVariables);
        }

        try {
          Template template = liqpParser.parse(in);

          contentSupply = template.renderToObjectUnguarded(variables);

          for (Exception exc : template.errors()) {
            if (LOG.isWarnEnabled()) {
              LOG.warn("Template error: {}", exc.toString());
            }
          }
        } catch (RuntimeException e) {
          throw new IllegalStateException("Error in layout " + layoutId, e);
        }

        in.close();
      }

      // the layout can have another layout
      layoutId = YAMLSupport.getVariableAsString(variables, "layout", "layout");
      in = layoutReader(layoutId);
      if (in == null) {
        break;
      }
    } while (true); // NOPMD.WhileLoopWithLiteralBoolean

    return contentSupply;
  }

  public static TemplateParser newLiqpParser(ServerApp app) {
    return new TemplateParser.Builder() //
        .withFlavor(Flavor.JEKYLL) //
        // filters
        .withFilter(new MarkdownifyFilter(new MarkdownHelper())) //
        .withFilter(new SlugifyFilter()) //
        .withFilter(new NumberOfWordsFilter()) //
        .withFilter(new DateToXmlschemaFilter()) //
        .withFilter(new JsonifyFilter())
        // tags
        .withInsertion(new JockelIncludeTag()) //
        .withInsertion(new SeoTag()) //
        .withInsertion(new AssetPathTag()) //
        .withInsertion(new Conditional()) //
        // blocks
        .withInsertion(new Conditionally()) //
        //
        // .withRenderTransformer(RenderTransformer.DEFAULT) //
        .withStrictVariables(StrictVariablesMode.SANE) //
        .withRenderTransformer(StringHolderRenderTransformer.getSharedCacheInstance()) //
        // .withRenderTransformer(StringsOnlyRenderTransformer.getInstance()) //
        .withShowExceptionsFromInclude(true) //
        .withEnvironmentMapConfigurator((env) -> {
          env.put(ENVIRONMENT_KEY_DUMBO_APP, app);
        }) //
        .withErrorMode(ErrorMode.WARN) //
        .build();
  }
}
