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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.DumboServerImpl;
import com.kohlschutter.dumbo.ServerApp;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class MarkdownServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(MarkdownServlet.class);
  private static final long serialVersionUID = 1L;
  private transient ServletContext servletContext;

  private transient ServerApp app;
  private transient MarkdownSupportImpl mdSupport;

  @Override
  public void init() throws ServletException {
    this.servletContext = getServletContext();
    this.app = Objects.requireNonNull(DumboServerImpl.getServerApp(servletContext));

    try {
      mdSupport = app.getImplementationByIdentity(MarkdownSupportImpl.COMPONENT_IDENTITY,
          () -> new MarkdownSupportImpl(app));
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    try {
      doGet0(req, resp);
    } catch (ServletException | IOException | RuntimeException e) {
      LOG.warn("Error in doGet", e);
      throw e;
    }
  }

  private void doGet0(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String servletPath = req.getServletPath();
    if (servletPath == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    URL url = servletContext.getResource(servletPath);
    if (url == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    Path mdPath;
    try {
      mdPath = Path.of(url.toURI());
    } catch (URISyntaxException e) {
      LOG.warn("Unexpected URI", e);
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (!Files.exists(mdPath) || Files.isDirectory(mdPath)) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    long mdFileLength = Files.size(mdPath);
    if (mdFileLength > Integer.MAX_VALUE) {
      // FIXME: use a lower bound
      resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      return;
    }

    String relativePath = servletPath;
    if (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }
    File mdFile = new File(app.getWebappWorkDir(), relativePath);

    boolean reload = "true".equals(req.getParameter("reload"));
    mdSupport.render(true, relativePath, mdPath, mdFile, reload, resp, null, null, null);
  }
}
