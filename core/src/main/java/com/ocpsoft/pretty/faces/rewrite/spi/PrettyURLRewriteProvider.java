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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIParameter;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.PrettyException;
import com.ocpsoft.pretty.faces.config.mapping.PathParameter;
import com.ocpsoft.pretty.faces.config.mapping.UrlMapping;
import com.ocpsoft.pretty.faces.config.rewrite.RewriteRule;
import com.ocpsoft.pretty.faces.rewrite.RewriteType;
import com.ocpsoft.pretty.faces.servlet.PrettyFacesWrappedRequest;
import com.ocpsoft.pretty.faces.spi.RewriteProvider;
import com.ocpsoft.pretty.faces.url.QueryString;
import com.ocpsoft.pretty.faces.url.URL;
import com.ocpsoft.pretty.faces.util.PrettyURLBuilder;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class PrettyURLRewriteProvider implements RewriteProvider
{
   private static final Log log = LogFactory.getLog(PrettyURLRewriteProvider.class);

   /**
    * Apply the given list of {@link RewriteRule}s to the URL (in order,) perform a redirect/forward if required.
    * Canonicalization is only invoked if it has not previously been invoked on this request. This method operates on
    * the requestUri, excluding contextPath.
    */
   public RewriteType rewriteInbound(final PrettyContext context, final HttpServletRequest request,
            final HttpServletResponse response)
   {
      try
      {
         URL url = context.getRequestURL();
         if (context.getConfig().isURLMapped(url))
         {
            String viewId = context.getCurrentViewId();
            if (!response.isCommitted())
            {
               if (context.shouldProcessDynaview())
               {
                  log.trace("Forwarding mapped request [" + url.toURL() + "] to dynaviewId [" + viewId + "]");
                  request.getRequestDispatcher(context.getDynaViewId()).forward(request, response);
                  return RewriteType.FORWARD;
               }
               else
               {
                  List<PathParameter> params = context.getCurrentMapping().getPatternParser().parse(url);
                  QueryString query = QueryString.build(params);

                  ServletRequest wrappedRequest = new PrettyFacesWrappedRequest(request, query.getParameterMap());

                  log.trace("Sending mapped request [" + url.toURL() + "] to resource [" + viewId + "]");
                  if (url.decode().toURL().matches(viewId))
                  {
                     return RewriteType.CONTINUE;
                  }
                  else
                  {
                     request.getRequestDispatcher(viewId).forward(wrappedRequest, response);
                     return RewriteType.FORWARD;
                  }
               }
            }
         }
         else
         {
            log.trace("Request is not mapped using PrettyFaces. Continue.");
         }
         return RewriteType.CONTINUE;
      }
      catch (Exception e)
      {
         throw new PrettyException("Could not forward request.", e);
      }
   }

   public String rewriteOutbound(final PrettyContext context, final HttpServletRequest request,
            final HttpServletResponse response, final String url)
   {
      String result = url;

      if (url != null)
      {
         String contextPath = request.getContextPath();
         String strippedUrl = stripContextPath(contextPath, url);

         List<UrlMapping> matches = new ArrayList<UrlMapping>();
         for (UrlMapping m : context.getConfig().getMappings())
         {
            if (!"".equals(m.getViewId()) && strippedUrl.startsWith(m.getViewId()))
            {
               matches.add(m);
            }
         }

         Collections.sort(matches, ORDINAL_COMPARATOR);

         Iterator<UrlMapping> iterator = matches.iterator();
         while (iterator.hasNext())
         {
            UrlMapping m = iterator.next();

            if (m.isOutbound())
            {
               List<UIParameter> uiParams = new ArrayList<UIParameter>();

               QueryString qs = QueryString.build("");
               if (url.contains("?"))
               {
                  qs.addParameters(url);
               }
               Map<String, String[]> queryParams = qs.getParameterMap();

               List<PathParameter> pathParams = m.getPatternParser().getPathParameters();

               int pathParamsFound = 0;
               for (PathParameter p : pathParams)
               {
                  UIParameter uip = new UIParameter();
                  String[] values = queryParams.get(p.getName());
                  if ((values != null) && (values.length > 0))
                  {
                     String value = values[0];
                     uip.setValue(value);
                     if ((value != null) && !"".equals(value))
                     {
                        pathParamsFound++;
                     }
                  }
                  queryParams.remove(p.getName());
                  uiParams.add(uip);
               }

               for (Entry<String, String[]> entry : queryParams.entrySet())
               {
                  UIParameter uip = new UIParameter();
                  uip.setName(entry.getKey());
                  uip.setValue(entry.getValue());
                  uiParams.add(uip);
               }

               if (pathParams.size() == pathParamsFound)
               {
                  PrettyURLBuilder builder = new PrettyURLBuilder();
                  result = contextPath + builder.build(m, true, uiParams);
                  break;
               }
            }
         }
      }
      return result;
   }

   private static final Comparator<UrlMapping> ORDINAL_COMPARATOR = new Comparator<UrlMapping>()
   {
      public int compare(final UrlMapping l, final UrlMapping r)
      {
         if (l.getPatternParser().getParameterCount() < r.getPatternParser().getParameterCount())
         {
            return 1;
         }
         else if (l.getPatternParser().getParameterCount() > r.getPatternParser().getParameterCount())
         {
            return -1;
         }
         return 0;
      }
   };

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

}
