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
package com.ocpsoft.pretty.faces.rewrite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.spi.RewriteProvider;
import com.ocpsoft.pretty.faces.util.ServiceLoader;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class RewriteEngine implements RewriteProvider
{
   private static List<RewriteProvider> providers = null;
   private final Comparator<RewriteProvider> providerPriorityComparator = new Comparator<RewriteProvider>()
   {

      public int compare(final RewriteProvider l, final RewriteProvider r)
      {
         // TODO implement rewrite priority
         return 0;
      }
   };

   private void init()
   {
      if (providers == null)
      {
         ServiceLoader<RewriteProvider> loader = ServiceLoader.load(RewriteProvider.class);

         providers = new ArrayList<RewriteProvider>();
         for (RewriteProvider provider : loader)
         {
            providers.add(provider);
         }

         Collections.sort(providers, providerPriorityComparator);
      }
   }

   public RewriteType rewriteInbound(final PrettyContext context, final HttpServletRequest request,
            final HttpServletResponse response)
   {
      init();
      for (RewriteProvider provider : providers)
      {
         RewriteType result = provider.rewriteInbound(context, request, response);
         if (!RewriteType.CONTINUE.equals(result))
         {
            return result;
         }
         else if (RewriteType.FINAL.equals(result))
            break;
      }
      return RewriteType.CONTINUE;
   }

   public String rewriteOutbound(final PrettyContext context, final HttpServletRequest request,
            final HttpServletResponse response,
            final String url)
   {
      init();

      String result = url;
      for (RewriteProvider provider : providers)
      {
         result = provider.rewriteOutbound(context, request, response, result);
      }
      return result;
   }
}
