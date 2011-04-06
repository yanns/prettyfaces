/*
 * Copyright 2010 Lincoln Baxter, III
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
package com.ocpsoft.pretty;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ocpsoft.pretty.faces.config.PrettyConfig;
import com.ocpsoft.pretty.faces.config.PrettyConfigurator;
import com.ocpsoft.pretty.faces.rewrite.RewriteEngine;
import com.ocpsoft.pretty.faces.rewrite.RewriteType;
import com.ocpsoft.pretty.faces.servlet.PrettyFacesWrappedResponse;

/**
 * @author Lincoln Baxter, III <lincoln@ocpsoft.com>
 */
public class PrettyFilter implements Filter
{
   private static final Log log = LogFactory.getLog(PrettyFilter.class);
   private final RewriteEngine rewrite = new RewriteEngine();

   private ServletContext servletContext;

   /**
    * Determine if the current request is mapped using PrettyFaces. If it is, process the pattern, storing parameters
    * into the request map, then forward the request to the specified viewId.
    */
   public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
            throws IOException, ServletException
   {
      HttpServletRequest request = (HttpServletRequest) req;
      HttpServletResponse response = new PrettyFacesWrappedResponse(request, (HttpServletResponse) resp);

      req.setAttribute(PrettyContext.CONFIG_KEY, getConfig());

      PrettyContext context = PrettyContext.newDetachedInstance(request);
      PrettyContext.setCurrentContext(request, context); // set

      RewriteType type = rewrite.rewriteInbound(context, request, response);

      if (RewriteType.CONTINUE.equals(type))
      {
         chain.doFilter(request, response);
      }

      if (resp.isCommitted())
      {
         log.trace("Rewrite occurred, reponse is committed - ending request.");
      }
   }

   public PrettyConfig getConfig()
   {
      if ((servletContext == null) || (servletContext.getAttribute(PrettyContext.CONFIG_KEY) == null))
      {
         log.warn("PrettyFilter is not registered in web.xml, but is registered with JSF "
                  + "Navigation and Action handlers -- this could cause unpredictable behavior.");
         return new PrettyConfig();
      }
      return (PrettyConfig) servletContext.getAttribute(PrettyContext.CONFIG_KEY);
   }

   /**
    * Load and cache configurations
    */
   public void init(final FilterConfig filterConfig) throws ServletException
   {
      log.info("PrettyFilter starting up...");
      servletContext = filterConfig.getServletContext();

      PrettyConfigurator configurator = new PrettyConfigurator(servletContext);
      configurator.configure();

      log.info("PrettyFilter initialized.");
   }

   public void destroy()
   {
      log.info("PrettyFilter shutting down...");
   }
}