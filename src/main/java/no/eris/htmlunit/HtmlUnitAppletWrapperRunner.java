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

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import no.eris.applet.AppletViewer;

import java.applet.Applet;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
* Created by IntelliJ IDEA.
*
* @author jerome@coffeebreaks.org
* @since Nov 26, 2010 1:54:13 PM
*/
public class HtmlUnitAppletWrapperRunner {
  static URI htmlUnitCookieToURI(com.gargoylesoftware.htmlunit.util.Cookie cookie) throws URISyntaxException {
    String scheme = cookie.isSecure() ? "https" : "http";
    String authority = cookie.getDomain();
    return new URI(scheme, authority, null, null, null);
  }
  static HttpCookie htmlUnitCookieToHttpCookie(com.gargoylesoftware.htmlunit.util.Cookie cookie){
    HttpCookie res = new HttpCookie(cookie.getName(), cookie.getValue());
    res.setDomain(cookie.getDomain());
    res.setPath(cookie.getPath());
    res.setPath(cookie.getPath());
    res.setSecure(cookie.isSecure());
    return res;
  }
  static AppletViewer.UriAndCookie htmlUnitCookieToURIAndCookie(com.gargoylesoftware.htmlunit.util.Cookie cookie) throws URISyntaxException {
    return new AppletViewer.UriAndCookie(htmlUnitCookieToURI(cookie), htmlUnitCookieToHttpCookie(cookie));
  }

  static com.gargoylesoftware.htmlunit.util.Cookie httpCookieToHtmlUnitCookie(HttpCookie cookie){
		return new com.gargoylesoftware.htmlunit.util.Cookie(
		 cookie.getDomain(),
		 cookie.getName(),
		 cookie.getValue(),
		 cookie.getPath(),
		 (int) cookie.getMaxAge(),
		 cookie.getSecure());
  }

  public HtmlPage run(final HtmlUnitAppletWrapper appletWrapper) throws Exception {
    List<AppletViewer.UriAndCookie> uriAndCookies = getUriAndCookies(appletWrapper);
    //println("Nb cookies found: " + uriAndCookies.size)

    // My applet viewer
    AppletViewer viewer = new AppletViewer(appletWrapper.attributes, appletWrapper.params);
    //viewer.setLoadFromLocalClasspath(true);
    viewer.setCookiesToAddToStore(uriAndCookies);
    final ShowDocumentResult result = new ShowDocumentResult();
    viewer.setShowDocumentListener(new AppletViewer.ShowDocumentListener() {
      public void showDocument(URL url, String frame) {
        try{
          HtmlPage page = appletWrapper.showDocument(url, frame);
          result.page = page;
        } catch(IOException e){
          result.e = e;
        }
      }
    });
    Thread t = viewer.run(false);
    if (t != null) {
      while (t.isAlive()) {
        try{
          Thread.sleep(200);
        } catch(InterruptedException e){
          e.printStackTrace();
        }
      }
    } else {
      viewer.waitForAppletToClose();
    }
		transferCookiesFromAppletToHtmlUnit(appletWrapper, viewer);
    viewer.disposeResources();
    if (!result.isSet()) {
      return null;
    }
    Applet applet = viewer.getRunningApplet();
    if (result.e != null) {
      throw result.e;
    }
    return result.page;
  }

  private List<AppletViewer.UriAndCookie> getUriAndCookies(HtmlUnitAppletWrapper appletWrapper) throws URISyntaxException {
    List<AppletViewer.UriAndCookie> uriAndCookies = new ArrayList<AppletViewer.UriAndCookie>();
    WebClient webClient = appletWrapper.applet.getPage().getWebClient();
    com.gargoylesoftware.htmlunit.CookieManager cookieManager = webClient.getCookieManager();
    for(com.gargoylesoftware.htmlunit.util.Cookie cookie: cookieManager.getCookies()){
      uriAndCookies.add(htmlUnitCookieToURIAndCookie(cookie));
    }
    return uriAndCookies;
  }

	private void transferCookiesFromAppletToHtmlUnit(final HtmlUnitAppletWrapper appletWrapper, AppletViewer viewer){
    WebClient webClient = appletWrapper.applet.getPage().getWebClient();
    com.gargoylesoftware.htmlunit.CookieManager htmlUnitCookieManager = webClient.getCookieManager();
		for (HttpCookie cookie: viewer.getCookieStore().getCookies()){
				htmlUnitCookieManager.addCookie(httpCookieToHtmlUnitCookie(cookie));
		}
	}							

  public static class ShowDocumentResult {
    public HtmlPage page;
    public Exception e;
    public boolean isSet() {
      return page != null || e != null;
    }
  }
}
