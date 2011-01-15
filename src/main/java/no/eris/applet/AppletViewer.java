
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

package no.eris.applet;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ClasspathUtils;
import util.ThreadUtils;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A Single AppletViewer should be created per applet we are about to run.
 *
 * @author jerome@coffeebreaks.org
 * @since Sep 23, 2010 7:14:16 PM
 */
public class AppletViewer {
  private static Logger LOGGER = LoggerFactory.getLogger(AppletViewer.class);
  /**
   * Called back when the applet triggers it
   */
  public interface ShowDocumentListener {
    void showDocument(URL url, String frame);
  }
	final CookieManager manager = new CookieManager();

  // simple cookie handled used to diagnose cookie issues
  private static class LoggingCookieHandler extends CookieHandler {
    private CookieManager manager;
		public CookieManager getManager() {
				return this.manager;
		}
    public LoggingCookieHandler(CookieManager manager) {
      this.manager = manager;
    }
    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
      Map<String, List<String>> result = manager.get(uri, requestHeaders);
      LOGGER.debug("GET cookie from {} headers= {}", new Object[] {uri, result, requestHeaders});
			for (String key: result.keySet()) {
		      LOGGER.debug(key + "=" + result.get(key));
			}
      return result;
    }
    @Override
    public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
      LOGGER.debug("PUT cookie from {} headers= {}", uri, responseHeaders);
			for(String key: responseHeaders.keySet()){
					if (key.toLowerCase().equals("set-cookie"))
				      LOGGER.debug(key + "=" + responseHeaders.get(key));
			}	
      manager.put(uri, responseHeaders);
    }
  }

  private JFrame frame;
  private AppletAdapter appletAdapter;
  private Map<String, String> attributes;
  private Map<String, String> params;

  private Class appletClass = null;
  /** The Applet instance we are running, or null. Can not be a JApplet
   * until all the entire world is converted to JApplet. */
  Applet applet = null;

  private ThreadGroup threadGroup = null;

  /* By default we load the applet remotely */
  private boolean loadFromLocalClasspath = false;
  private List<UriAndCookie> cookies;

  private ShowDocumentListener showDocumentListener;

  public static void main(String[] av) {
    if (av.length < 2) {
      throw new IllegalArgumentException("USAGE: AppletViewer configFileAttributes configFileParams");
    }
    Thread t = AppletViewer.build(av[0], av[1]).run(true);
    if (t != null) {
      while (t.isAlive()) {
        try{
          Thread.sleep(5000);
        } catch(InterruptedException e){
          e.printStackTrace(System.err);
        }
      }
    }
  }

  public AppletViewer(Map<String, String> attributes, Map<String, String>params) {
    this.attributes = attributes;
    this.params = params;
  }

  public void setLoadFromLocalClasspath(boolean loadFromLocalClasspath) {
    this.loadFromLocalClasspath = loadFromLocalClasspath;
  }

  public void setShowDocumentListener(ShowDocumentListener showDocumentListener) {
    this.showDocumentListener = showDocumentListener;
  }

  public void showDocument(URL url, String frame) {
    if (showDocumentListener != null) {
      showDocumentListener.showDocument(url, frame);
    }
  }

  /**
   * Constructs the GUI for an Applet Viewer
   * @param configPropFileAttributes the property file containing the attributes
   * @param configPropFileParams the property file containing the parameters
   * @return the AppletViewer
   */
  static AppletViewer build(String configPropFileAttributes, String configPropFileParams) {
    Map<String, String> attributes = readConfig(configPropFileAttributes, false);
    Map<String, String> params = readConfig(configPropFileParams, true);
    return new AppletViewer(attributes, params);
  }

  private static Map<String, String> readConfig(String configPropFile, boolean convertKeyToLowerCase) {
    Properties p = new Properties();
    try {
      p.load(AppletViewer.class.getResourceAsStream(configPropFile));
      Map<String, String> m = new HashMap<String, String>();
      for(String key:p.stringPropertyNames()) {
        if (convertKeyToLowerCase)
          m.put(key.toLowerCase(), p.getProperty(key));
        else
          m.put(key, p.getProperty(key));
      }
      return m;
    } catch(IOException e) {
      throw new IllegalStateException("Couldn't load properties from " + configPropFile, e);
    }
  }

  /**
   * Runs the applet. Creates a Frame and adds it to it.
   * @param async whether to start a separate thread running the viewer.
   * @return the started thread or <code>null</code> when async is false
   */
  public Thread run(final boolean async) {

    overrideCookieHandler(manager);

    frame = new JFrame("AppletViewer");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
		    LOGGER.debug("windowClosing");
        disposeResources();
      }
    });

    Container cp = frame.getContentPane();
    cp.setLayout(new BorderLayout());

    // Instantiate the AppletAdapter which gives us
    // AppletStub and AppletContext.
    if (appletAdapter == null)
      appletAdapter = new AppletAdapter(this, attributes, params);

    // The AppletAdapter also gives us showStatus.
    // Therefore, must add() it very early on, since the Applet's
    // Constructor or its init() may use showStatus()
    cp.add(BorderLayout.SOUTH, appletAdapter);

    showStatus("Loading Applet ");
    if (loadFromLocalClasspath) {
      loadAppletLocaly();
    } else {
      loadAppletRemotely();
    }
    setAppletSize();

    if (applet == null) {
      LOGGER.debug("applet null");
      return null;
    }

    // Now right away, tell the Applet how to find showStatus et al.
    applet.setStub(appletAdapter);

    // Connect the Applet to the Frame.
    cp.add(BorderLayout.CENTER, applet);

    threadGroup = new ThreadGroup("AppletViewer-" + applet.getParameter("name") + "-FIXME_ID");
    threadGroup.setDaemon(true);

    // Here we pretend to be a browser!
    final Runnable task = new Runnable() {
      public void run() {
        applet.init();
        final Dimension d = applet.getSize();
        d.height += appletAdapter.getSize().height;
        frame.setSize(d);
        frame.setVisible(true);    // make the Frame and all in it appear
        applet.start();
        showStatus("Applet " + applet.getParameter("name") + " loaded");
        if (async) {
          waitForAppletToClose();
        }
      }
    };
    if (async) {
      Thread t = new Thread(threadGroup, task);
      final ClassLoader loader = applet.getClass().getClassLoader();
      t.setContextClassLoader(loader);
      t.start();
      return t;
    } else {
      task.run();
      return null;
    }
  }
  private void setAppletSize() {
    final int height = Integer.parseInt(appletAdapter.getParameter("height"));
    final int width = Integer.parseInt(appletAdapter.getParameter("width"));
    applet.setSize(width, height);
  }
  public void waitForAppletToClose() {
    while(applet != null) {
      try{
        Thread.sleep(100);
        // Utils.printObjectGraph(applet);
      } catch (InterruptedException e) {
        LOGGER.error("waiting for applet to close", e);
      }
    }
  }

  public Applet getRunningApplet() {
    return applet;
  }

  public void disposeResources() {
    LOGGER.debug("Disposing resources...");
    if (frame != null) {
      frame.setVisible(false);
      frame.dispose();
      frame = null;
    }
    if (applet != null) {
      applet.stop();
      applet.destroy();
      applet = null;
    }
    if (threadGroup != null && !threadGroup.isDestroyed()) {
      int maxWait = 2000;
      LOGGER.debug("Threads not dead - waiting a bit...");
      long start = System.currentTimeMillis();
      while (System.currentTimeMillis() - start < maxWait) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          LOGGER.error("InterruptedException while waiting for threads to die", e);
        }
      }
    }
    destroyThreadGroup();
  }

  private void destroyThreadGroup() {
    if (threadGroup != null && !threadGroup.isDestroyed()) {
      LOGGER.debug("Threads not dead - killing them");
      Thread[] aliveThreads = null;
      boolean enumeratedAll = false;
      while (!enumeratedAll) {
        int countThreads = threadGroup.activeCount();
        aliveThreads = new Thread[countThreads + 2]; // take some margin
        int enumerated = threadGroup.enumerate(aliveThreads);
        enumeratedAll = enumerated < aliveThreads.length;
        if (!enumeratedAll)
          LOGGER.debug("Couldn't enumerate all threads");
      }
      for(Thread aliveThread : aliveThreads) {
        if (aliveThread != null && aliveThread.isAlive()) {
          ThreadUtils.forceInterrupt(aliveThread);
        }
      }
      if (threadGroup.activeCount() == 0) {
        threadGroup.destroy();
      } else {
        LOGGER.debug("Couldn't destroy thread group: not empty !");
        ThreadUtils.printAliveThreadStack();
      }
      threadGroup = null;
    }
  }

  public static class UriAndCookie {
    private URI uri;
    private HttpCookie cookie;
    public UriAndCookie(URI uri, HttpCookie cookie) {
      this.uri = uri;
      this.cookie = cookie;
    }
    public URI getUri() {
      return uri;
    }
    public HttpCookie getCookie() {
      return cookie;
    }
  }

  public void setCookiesToAddToStore(List<UriAndCookie> cookies) {
    this.cookies = cookies;
  }

	public CookieStore getCookieStore(){
		return manager.getCookieStore();
	}

  private void overrideCookieHandler(CookieManager manager) {
    manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    final CookieHandler handler = CookieHandler.getDefault();

    LOGGER.debug("CookieStore: size {}", manager.getCookieStore().getCookies().size());
    if (cookies != null) {
      for (UriAndCookie uriAndCookie: cookies) {
        URI uri = uriAndCookie.getUri();
        HttpCookie cookie = uriAndCookie.getCookie();
        LOGGER.debug("Adding cookies: <{}> value={} secure={}", new Object[] {uri, cookie, cookie.getSecure()});
        manager.getCookieStore().add(uri, cookie);
      }
    }
    LOGGER.debug("CookieStore: size {}", manager.getCookieStore().getCookies().size());
    LOGGER.debug("Overriding cookie handler: {}", (handler == null ? null : handler.getClass().getName()));
    // FIXME because we depend on the system-wide cookie manager, we probably cannot run multiple applets at the time
    // we also maybe have some security issues lurking here...
    // I could maybe partition the callers based on the ThreadGroup ?? 
    // FIXME theres also some cleanup to do somewhere
    CookieHandler.setDefault(new LoggingCookieHandler(manager));
  }

  void loadAppletRemotely() {
    String archive = appletAdapter.getParameter("archive");
    String codebase = appletAdapter.getParameter("codebase");
    final String appletName = appletAdapter.getParameter("code");

    String className = appletName;
    if (className.endsWith(".class")) {
      className = className.substring(0, className.length() - ".class".length());
    }

    final String baseUrl = codebase + (codebase.endsWith("/") ? "" : "/");
    final String url = baseUrl + (archive == null ? "" : archive);

    try {
      LOGGER.debug("Loading Applet from URL: {}", url);
      ClassLoader cl = new AppletClassLoader(new URL(url), archive, baseUrl, appletName);
      appletClass = Class.forName(className, true, cl);
      // Construct an instance (as if using no-argument constructor)
      applet = (Applet) appletClass.newInstance();
    } catch (Throwable t) {
      ClasspathUtils.displayClasspath(applet);
      final String message = "Applet " + appletName + " couldn't load from " + url;
      showStatus(message, new Exception(message, t));
    }
  }

  /*
   * Load the Applet into memory. Should do caching.
   */
  void loadAppletLocaly() {
    String appletName = appletAdapter.getParameter("code");
    if (appletName.endsWith(".class")) {
      appletName = appletName.substring(0, appletName.length() - ".class".length());
    }
    try {
      ClasspathUtils.displayClasspath(this);
      // get a Class object for the Applet subclass
      appletClass = Class.forName(appletName);
      // Construct an instance (as if using no-argument constructor)
      applet = (Applet) appletClass.newInstance();
    } catch(ClassNotFoundException e) {
      ClasspathUtils.displayClasspath(this);
      showStatus("Applet subclass " + appletName + " did not load", e);
    } catch (Exception e ) {
      ClasspathUtils.displayClasspath(this);
      showStatus("Applet " + appletName + " did not instantiate", e);
    } catch (Throwable t) {
      ClasspathUtils.displayClasspath(this);
      showStatus("Applet " + appletName + " did not instantiate", new Exception("...", t));
    }
  }

  public void showStatus(String s) {
    appletAdapter.getAppletContext().showStatus(s);
  }

  public void showStatus(String s, Exception e) {
	  LOGGER.error("showStatus: {}: {}", s, e.getMessage());
	  e.printStackTrace();
    appletAdapter.getAppletContext().showStatus(s);
  }

  private static class AppletClassLoader extends java.net.URLClassLoader {
    private final String archive;
    private final String baseUrl;
    private final String appletName;
    public AppletClassLoader(URL jarUrl, String archive, String baseUrl, String appletName) {
      super(new URL[]{jarUrl}, null);
      this.archive = archive;
      this.baseUrl = baseUrl;
      this.appletName = appletName;
    }
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      try {
        return super.findClass(name);
      } catch (Exception ignored) {
      }
      if (archive == null) {
        String fileUrl = baseUrl + appletName.replace('.', '/');
        if (!fileUrl.endsWith(".class")) {
          fileUrl += ".class";
        }
        try{
          LOGGER.debug("Reading Applet class from URL: {}", fileUrl);
          byte[] bytes = readBytes(new URL(fileUrl));
          return defineClass(name, bytes, 0, bytes.length);
        } catch(Exception e) {
          throw new ClassNotFoundException("Couldn't read from " + fileUrl, e);
        }
      }
      throw new ClassNotFoundException("We don't know where to search for class file...");
    }
    private byte[] readBytes(URL url) throws IOException {
      URLConnection s = url.openConnection();
      if (s instanceof HttpURLConnection) {
        HttpURLConnection httpUrlConnection = (HttpURLConnection) s;
        int responseCode = httpUrlConnection.getResponseCode();
        if (responseCode >= 400) {
          throw new IOException("Couldn't open " + url + " response: " + responseCode);
        }
      }
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      IOUtils.copy(s.getInputStream(), baos);
      return baos.toByteArray();
    }
  }
}

class AppletAdapter extends Panel implements AppletStub, AppletContext {
  private static Logger LOGGER = LoggerFactory.getLogger(AppletAdapter.class);

  /** The status window at the bottom */
  private Label status = null;

  private AppletViewer appletViewer;

  private Map<String, String> attributes;
  private Map<String, String> params;

  /** Construct the GUI for an Applet Status window */
  AppletAdapter(AppletViewer appletViewer, Map<String, String> attributes, Map<String, String> params) {
    this.appletViewer = appletViewer;
    this.attributes = attributes;
    this.params = params;

    // Must do this very early on, since the Applet's
    // Constructor or its init() may use showStatus()
    add(status = new Label());

    // Give "status" the full width
    status.setSize(getSize().width, status.getSize().height);

    showStatus("AppletAdapter constructed");  // now it can be said
  }

  /****************** AppletStub ***********************/
  /** Called when the applet wants to be resized.  */
  public void appletResize(int w, int h) {
    LOGGER.debug("appletResize {}, {}", w, h);
    // applet.setSize(w, h);
  }

  /** Gets a reference to the applet's context.  */
  public AppletContext getAppletContext() {
    // debug("getAppletContext()");
    return this;
  }

  /** Gets the base URL.  */
  public URL getCodeBase() {
    LOGGER.debug("getCodeBase()");
    return getClass().getResource(".");
  }

  /** Gets the document URL.  */
  public URL getDocumentBase() {
    LOGGER.debug("getDocumentBase()");
    return getClass().getResource(".");
  }

  /** Returns the value of the named parameter in the HTML tag.
   * Cases are not sensitive */
  public String getParameter(String name) {
    LOGGER.debug("getParameter({})", name);
    String value = params.get(name);
    // try lower case
    if (value == null) {
      value = params.get(name.toLowerCase());
    }
    // search attributes
    if (value == null) {
      value = attributes.get(name);
    }
    // search lower case attributes
    if (value == null) {
      value = attributes.get(name.toLowerCase());
    }
    if (value == null) {
      LOGGER.error("AppletViewer Param '{}' not passed neither as parameter nor as attribute!", name);
    }
    return value;
  }
  /** Determines if the applet is active.  */
  public boolean isActive() {
    LOGGER.debug("isActive()");
    return true;
  }

  /************************ AppletContext ************************/

  /** Finds and returns the applet with the given name. */
  public Applet getApplet(String an) {
    LOGGER.debug("getApplet({})", an);
    return appletViewer.applet;
  }

  /** Finds all the applets in the document */
  public Enumeration<Applet> getApplets()  {
    // LOGGER.debug("getApplets()");
    List<Applet> applets = new java.util.ArrayList<Applet>();
    applets.add(appletViewer.applet);
    return java.util.Collections.enumeration(applets);
  }

  /** Create an audio clip for the given URL of a .au file */
  public AudioClip getAudioClip(URL url) {
    LOGGER.debug("getAudioClip({})", url);
    return null;
  }

  /** Look up and create an Image object that can be paint()ed */
  public Image getImage(URL url)  {
    LOGGER.debug("getImage({})", url);
    return null;
  }

  /** Request to overlay the current page with a new one - passed to the listener */
  public void showDocument(URL url) {
    LOGGER.debug("showDocument({})", url);
    appletViewer.showDocument(url, null);
  }

  /** as above but with a Frame target */
  public void showDocument(URL url, String frame)  {
    LOGGER.debug("showDocument({}, {})", url, frame);
    appletViewer.showDocument(url, frame);
  }

  /** Called by the Applet to display a message in the bottom line */
  public void showStatus(String msg) {
    LOGGER.debug("showStatus({})", msg);
    if (msg == null)
      msg = "";
    status.setText(msg);
  }

  /* StreamKey stuff - new in JDK1.4 */
  private HashMap<String, InputStream> streamMap = new HashMap<String, InputStream>();

  /** Associate the stream with the key. */
  public void setStream(String key, InputStream stream) throws IOException {
    LOGGER.debug("setStream({}, {})", key, stream);
    streamMap.put(key, stream);
  }

  public InputStream getStream(String key) {
    LOGGER.debug("getStream({})", key);
    return streamMap.get(key);
  }

  public Iterator<String> getStreamKeys() {
    LOGGER.debug("getStreamKeys()");
    return streamMap.keySet().iterator();
  }
}