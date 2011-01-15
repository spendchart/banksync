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

package no.eris.htmlunit;

import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlParameter;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Created by IntelliJ IDEA.
*
* @author jerome@coffeebreaks.org
* @since Nov 26, 2010 9:04:15 AM
*/
public class HtmlUnitAppletWrapper {
  public HtmlElement applet;
  public Map<String, String> attributes;
  public Map<String, String> params;

  private boolean fakeReferer = false;

  public HtmlUnitAppletWrapper(HtmlElement applet, Map<String, String> attributes, Map<String, String> params) {
    this.applet = applet;
    this.attributes = attributes;
    this.params = params;
  }

  public static <X,Y> Y getOrElse(Map<X, Y> map, X value, Y alternative) {
    if (map.containsKey(value)) {
      return map.get(value);
    } else
      return alternative;
  }
  public String name() { return getOrElse(attributes, "name", null); } ;
  public String parameter(String name) { return getOrElse(params, name.toLowerCase(), null); };
  public String attribute(String name) { return getOrElse(attributes, name, null); }


  // FIXME this API isn't stable
  public void setFakeReferer(boolean fakeReferer) {
    this.fakeReferer = fakeReferer;
  }

  public HtmlPage getPage(String url, String target) {
    String js = "window.open(\"" + url + "\", \"" + target+ "\");";
    println(js);
    ScriptResult res = ((HtmlPage)applet.getPage()).executeJavaScript(js);
    return (HtmlPage)res.getNewPage();
    // applet.getPage.getWebClient.getPage(url).asInstanceOf[HtmlPage]
  }

  public HtmlPage showDocument(URL url, String frame) throws IOException {
    // we fake the referer
    // https://nettbanken.nordea.no/login/login/solo/bankid/no.bbs.bankid.client.gui.ClientApplet.class
    // i.e. param(URL) + param(code)
    if (frame == null || frame.equals("_self")) {
      WebRequest request = new WebRequest(url);
      if (fakeReferer) {
        String urlParam = parameter("URL");
        String referer = urlParam.substring(0, urlParam.lastIndexOf('/') + 1) + parameter("code");
        request.setAdditionalHeader("referer", referer);
        println("Using referer: " + referer);
      }
      return (HtmlPage)applet.getPage().getWebClient().getPage(request);
    } else return getPage(url.toString(), frame);
  }

  public static HtmlUnitAppletWrapper findAppletByName(HtmlPage page, String appletName) {
    return findAppletBy(page, "name", appletName);
  }

  public static HtmlUnitAppletWrapper findAppletByCode(HtmlPage page, String code) {
    return findAppletBy(page, "code", code);
  }

  public static HtmlUnitAppletWrapper findAppletBy(HtmlPage page, String key, String value) {
    HtmlElement applet = page.getFirstByXPath("//applet[@"+ key + "='"+ value + "']");

    if (applet == null) {
      // try finding it as object
      applet = page.getFirstByXPath("//object[@" + key + "='"+ value + "']");
    }
    if (applet == null) {
      // why isn't XPath able to find them otherwise ?
      applet = page.getFirstByXPath("//object[@" + key + "=\""+ value + "\"]");
    }
    if (applet == null) {
      println("Applet NOT found on page " + page.getTitleText());
      return null;
    }
    println("Applet found on page " + page.getTitleText() + ": " +  applet);

    Map<String, String> attributes = new HashMap<String, String>();
    Map<String, String> params = new HashMap<String, String>();

    for(String att : applet.getAttributesMap().keySet()){
      attributes.put(att, applet.getAttribute(att));
    }
    // applet.getByXPath("//param").asInstanceOf[java.util.List[HtmlParameter]].foreach( println(_))
    for(HtmlParameter parameter : (List<HtmlParameter>) applet.getByXPath("//param")){
      params.put(parameter.getNameAttribute().toLowerCase(), parameter.getValueAttribute());
    }

    //println("Applet attributes: " + attributes.toString());
    //println("Applet params: " + params.toString());
    return new HtmlUnitAppletWrapper(applet, attributes, params);
  }

  private static void println(String s) {
    System.out.println(s);
  }
}
