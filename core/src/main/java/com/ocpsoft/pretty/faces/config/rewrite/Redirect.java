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

package com.ocpsoft.pretty.faces.config.rewrite;

import com.ocpsoft.pretty.faces.rewrite.RewriteType;

/**
 * Enumeration describing different methods of Inbound Redirecting
 * 
 * @author Lincoln Baxter, III <lincoln@ocpsoft.com>
 */
public enum Redirect
{
    PERMANENT(301), TEMPORARY(302), CHAIN;

   private final int status;

   private Redirect()
   {
      status = -1;
   }

   private Redirect(final int status)
   {
      this.status = status;
   }

   public RewriteType getRewriteType()
   {
      switch (this)
      {
      case PERMANENT:
         return RewriteType.REDIRECT_301;
      case TEMPORARY:
         return RewriteType.REDIRECT_302;
      default:
         return RewriteType.FORWARD;
      }
   }

   public int getStatus()
   {
      return status;
   }
}
