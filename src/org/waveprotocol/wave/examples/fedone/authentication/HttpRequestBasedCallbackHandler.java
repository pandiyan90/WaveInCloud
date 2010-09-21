/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.wave.examples.fedone.authentication;

import org.eclipse.jetty.util.MultiMap;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * A CallbackHandler which configures callbacks based on a set of parameters
 * sent in an HTTP request.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class HttpRequestBasedCallbackHandler implements CallbackHandler {
  MultiMap<String> parameters;
  
  public HttpRequestBasedCallbackHandler(MultiMap<String> parameters) {
    this.parameters = parameters;
  }
  
  @Override
  public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
    for (Callback c : callbacks) {
      if (c instanceof NameCallback){
        if (parameters.containsKey("username")) {
          ((NameCallback) c).setName(parameters.getString("username"));
        }
      } else if (c instanceof PasswordCallback) {
        if (parameters.containsKey("password")) {
          String password = parameters.getString("password");
          ((PasswordCallback) c).setPassword(password.toCharArray());
        }
      } else {
        throw new UnsupportedCallbackException(c);
      }
    }
  }
}
