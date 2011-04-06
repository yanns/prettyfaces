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

package com.ocpsoft.pretty.faces.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.rewrite.RewriteEngine;

/**
 * @author Lincoln Baxter, III <lincoln@ocpsoft.com>
 */
public class PrettyFacesWrappedResponse extends HttpServletResponseWrapper
{
   private final HttpServletRequest request;

   public PrettyFacesWrappedResponse(final HttpServletRequest request, final HttpServletResponse response)
   {
      super(response);
      this.request = request;
   }

   @Override
   @SuppressWarnings("deprecation")
   public String encodeRedirectUrl(final String url)
   {
      return super.encodeRedirectUrl(url);
   }

   @Override
   public String encodeRedirectURL(final String url)
   {
      return super.encodeRedirectURL(url);
   }

   @Override
   @SuppressWarnings("deprecation")
   public String encodeUrl(final String url)
   {
      return super.encodeUrl(url);
   }

   @Override
   public String encodeURL(final String url)
   {
      // FIXME Threading issues potential?
      return new RewriteEngine().rewriteOutbound(PrettyContext.getCurrentInstance(request), request, this, url);
   }
}
