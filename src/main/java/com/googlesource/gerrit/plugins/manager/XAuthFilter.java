// Copyright (C) 2015 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.manager;

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSession;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

@Singleton
public class XAuthFilter implements Filter {
  private static final Logger log = LoggerFactory.getLogger(XAuthFilter.class);

  private DynamicItem<WebSession> webSession;

  @Inject
  public XAuthFilter(DynamicItem<WebSession> webSession) {
    this.webSession = webSession;
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp,
      FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpReq = (HttpServletRequest) req;
    HttpServletResponse httpResp = (HttpServletResponse) resp;

    final String gerritAuth = webSession.get().getXGerritAuth();
    if (gerritAuth != null) {
      log.debug("Injecting X-Gerrit-Auth for {}", httpReq.getRequestURI());
      httpResp = new HttpServletResponseWrapper(httpResp) {

        private int origContentLength;

        @Override
        public void setHeader(String name, String value) {
          log.debug("{}: {}", name, value);
          if (name.equalsIgnoreCase("Content-Length")) {
            origContentLength = Integer.parseInt(value);
          } else {
            super.setHeader(name, value);
          }
        }

        @Override
        public PrintWriter getWriter() throws IOException {
          return super.getWriter();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
          return new TokenReplaceOutputStream(
              (HttpServletResponse) getResponse(), origContentLength,
              "@X-Gerrit-Auth".getBytes(), gerritAuth.getBytes());
        }
      };
    }

    chain.doFilter(req, httpResp);
  }

  @Override
  public void init(FilterConfig conf) throws ServletException {
  }
}
