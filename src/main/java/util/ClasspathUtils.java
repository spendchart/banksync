/*
 * OpenNetbank a client to Free your bank information.
 * Copyright (C) 2010  Jerome Lacoste <jerome@coffeebreaks.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util;

/**
 * Created by IntelliJ IDEA.
 *
 * @author jerome@coffeebreaks.org
 * @since Sep 12, 2010 5:18:24 PM
 */
public class ClasspathUtils {
  public static void displayClasspath(Object o) {
    if (o == null) {
      return;
    }
    System.out.println("URLs in URLClassLoader: ");
    if (o.getClass().getClassLoader() instanceof java.net.URLClassLoader)
      System.out.println(java.util.Arrays.toString(((java.net.URLClassLoader)o.getClass().getClassLoader()).getURLs()).replace(":", "\n"));

  }
}
