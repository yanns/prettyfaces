/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.ocpsoft.pretty.faces.rewrite.spi;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.PrettyException;
import com.ocpsoft.pretty.faces.config.rewrite.Redirect;
import com.ocpsoft.pretty.faces.config.rewrite.RewriteRule;
import com.ocpsoft.pretty.faces.rewrite.RewriteProcessorRunner;
import com.ocpsoft.pretty.faces.rewrite.RewriteType;
import com.ocpsoft.pretty.faces.spi.RewriteProvider;
import com.ocpsoft.pretty.faces.url.QueryString;
import com.ocpsoft.pretty.faces.url.URL;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class RewriteRuleRewriteProvider implements RewriteProvider
{
   private static final String REWRITE_OCCURRED_KEY = "com.ocpsoft.pretty.rewrite";

   /**
    * Apply the given list of {@link RewriteRule}s to the URL (in order,) perform a redirect/forward if required.
    * Canonicalization is only invoked if it has not previously been invoked on this request. This method operates on
    * the requestUri, excluding contextPath.
    */
   public RewriteType rewriteInbound(final PrettyContext context, final HttpServletRequest request,
            final HttpServletResponse response)
   {
      RewriteType result = RewriteType.CONTINUE;
      /*
       * FIXME Refactor this horrible method.
       */
      if (!rewriteOccurred(request))
      {
         RewriteProcessorRunner rewriteEngine = new RewriteProcessorRunner();
         URL url = context.getRequestURL();

         try
         {

            String queryString = request.getQueryString();
            if ((queryString != null) && !"".equals(queryString))
            {
               queryString = "?" + queryString;
            }
            else if (queryString == null)
            {
               queryString = "";
            }

            // TODO test this now that query string is included in rewrites
            String originalUrl = url.toURL() + queryString;
            String newUrl = originalUrl;
            for (RewriteRule rule : context.getConfig().getGlobalRewriteRules())
            {
               if (rule.matches(newUrl))
               {
                  newUrl = rewriteEngine.processInbound(request, response, rule, newUrl);
                  Redirect status = rule.getRedirect();
                  result = status.getRewriteType();

                  if (!Redirect.CHAIN.equals(status))
                  {
                     /*
                      * An HTTP redirect has been triggered; issue one if we have a url or if the current url has been
                      * modified.
                      */
                     String ruleUrl = rule.getUrl();
                     if (((ruleUrl == null) || "".equals(ruleUrl.trim())) && !originalUrl.equals(newUrl))
                     {
                        /*
                         * The current URL has been rewritten - do redirect
                         */

                        // search for the '?' character
                        String[] parts = newUrl.split("\\?", 2);

                        // build URL from everything before the '?'
                        URL encodedPath = new URL(parts[0]).encode();
                        encodedPath.setEncoding(url.getEncoding());

                        // no query parameters, just a plain URL
                        if ((parts.length < 2) || (parts[1] == null) || "".equals(parts[1]))
                        {
                           newUrl = encodedPath.toURL();
                        }
                        // we found query parameters, so we append them in encoded representation
                        else
                        {
                           newUrl = encodedPath.toURL() + QueryString.build(parts[1]).toQueryString();
                        }

                        // send redirect
                        String redirectURL = response.encodeRedirectURL(request.getContextPath() + newUrl);
                        response.setHeader("Location", redirectURL);
                        response.setStatus(status.getStatus());
                        response.flushBuffer();
                        break;
                     }
                     else if ((ruleUrl != null) && !"".equals(ruleUrl.trim()))
                     {
                        /*
                         * This is a custom location - don't call encodeRedirectURL() and don't add context path, just
                         * redirect to the encoded URL
                         */
                        URL encodedNewUrl = new URL(newUrl).encode();
                        response.setHeader("Location", encodedNewUrl.toURL());
                        response.setStatus(status.getStatus());
                        response.setCharacterEncoding(url.getEncoding());
                        response.flushBuffer();
                        break;
                     }
                  }

               }
            }

            if (!originalUrl.equals(newUrl) && !response.isCommitted())
            {
               /*
                * The URL was modified, but no redirect occurred; forward instead.
                */
               request.getRequestDispatcher(newUrl).forward(request, response);
            }
         }
         catch (Exception e)
         {
            throw new PrettyException("Error occurred during canonicalization of request <[" + url + "]>", e);
         }
         finally
         {
            setRewriteOccurred(request);
         }
      }
      return result;
   }

   public String rewriteOutbound(final PrettyContext context, final HttpServletRequest request,
            final HttpServletResponse response, final String url)
   {
      RewriteProcessorRunner rewriteEngine = new RewriteProcessorRunner();
      String result = "";
      if (url != null)
      {
         String contextPath = request.getContextPath();
         String strippedUrl = stripContextPath(contextPath, url);

         if (!strippedUrl.equals(url))
         {
            result = contextPath;
         }

         try
         {
            for (RewriteRule c : context.getConfig().getGlobalRewriteRules())
            {
               strippedUrl = rewriteEngine.processOutbound(request, response, c, strippedUrl);
            }
            result += strippedUrl;
         }
         catch (Exception e)
         {
            throw new PrettyException("Error occurred during canonicalization of request <[" + url + "]>", e);
         }
      }
      return result;
   }

   /**
    * If the given URL is prefixed with this request's context-path, return the URI without the context path. Otherwise
    * return the URI unchanged.
    * 
    * @param url
    */
   private String stripContextPath(final String contextPath, String uri)
   {
      if (uri.startsWith(contextPath))
      {
         uri = uri.substring(contextPath.length());
      }
      return uri;
   }

   private void setRewriteOccurred(final ServletRequest req)
   {
      req.setAttribute(REWRITE_OCCURRED_KEY, true);
   }

   private boolean rewriteOccurred(final ServletRequest req)
   {
      return Boolean.TRUE.equals(req.getAttribute(REWRITE_OCCURRED_KEY));
   }

}
