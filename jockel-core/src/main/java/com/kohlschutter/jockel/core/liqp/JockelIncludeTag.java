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
package com.kohlschutter.jockel.core.liqp;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.jockel.core.LiquidHelper;

import liqp.Template;
import liqp.TemplateContext;
import liqp.nodes.LNode;
import liqp.parser.Flavor;
import liqp.tags.Tag;

public class JockelIncludeTag extends Tag {
  public static final String DEFAULT_EXTENSION = ".liquid";

  public JockelIncludeTag() {
    super("include");
  }

  @Override
  public Object render(TemplateContext context, LNode... nodes) {
    @SuppressWarnings("resource")
    ServerApp app = (ServerApp) Objects.requireNonNull(context.getEnvironmentMap().get(
        LiquidHelper.ENVIRONMENT_KEY_DUMBO_APP));

    String includeResource = null;
    try {
      includeResource = super.asString(nodes[0].render(context), context);
      if (includeResource.isEmpty()) {
        throw new FileNotFoundException("Can't include " + nodes[0] + " (empty string)");
      }

      if (includeResource.indexOf('.') == 0) {
        includeResource += DEFAULT_EXTENSION;
      }

      URL resource = app.getResource("markdown/_includes/" + includeResource);
      if (resource == null) {
        throw new FileNotFoundException("Can't include " + includeResource);
      }

      Map<String, Object> variables = new HashMap<String, Object>();

      // CustomSiteVariables.copyPathAndFileName(context.getVariables(), variables);

      try (Reader in = new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)) {
        Template template;
        template = context.getParser().parse(in);
        if (nodes.length > 1) {
          if (context.getParser().flavor != Flavor.JEKYLL) {
            // check if there's an optional "with expression"
            Object value = nodes[1].render(context);
            context.put(includeResource, value);
          } else {
            Map<String, Object> includeMap = new HashMap<>();
            variables.put("include", includeMap);
            for (int i = 1, n = nodes.length; i < n; i++) {
              @SuppressWarnings("unchecked")
              Map<String, Object> var = (Map<String, Object>) nodes[i].render(context);

              includeMap.putAll(var);
            }
          }
        }

        return template.renderToObjectUnguarded(variables, context, true);
      }
    } catch (Exception e) {
      e.printStackTrace();
      if (context.getParser().showExceptionsFromInclude) {
        throw new IllegalStateException("problem with evaluating include: " + includeResource, e);
      } else {
        return "";
      }
    }
  }
}
