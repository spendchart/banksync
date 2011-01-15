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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author jerome@coffeebreaks.org
 * @since Sep 25, 2010 12:01:11 PM
 */
public class ThreadUtils {
  public static Thread findAliveThreadWithStackContaining(String stackText) {
    Thread controllerThread = null;
    for (Map.Entry<Thread, StackTraceElement[]> entry: Thread.getAllStackTraces().entrySet()) {
      final Thread thread = entry.getKey();
      if (!thread.isDaemon()) {
        StackTraceElement[] s = entry.getValue();
        for(StackTraceElement element : s){
          if (element.toString().contains(stackText)) {
            controllerThread = thread;
            break;
          }
        }
      }
    }
    return controllerThread;
  }

  public static void printAliveThreadStack() {
    for (Map.Entry<Thread, StackTraceElement[]> entry: Thread.getAllStackTraces().entrySet()) {
      if (!entry.getKey().isDaemon()) {
        printThreadStack(entry.getKey(), entry.getValue(), System.out);
      }
    }
  }
  private static void printThreadStack(Thread thread, StackTraceElement[] stack, PrintStream out) {
    final String headerLine = "-----------------------------------------------------------------------";
    final String newLine = System.getProperty("line.separator");
    final String tab = "        ";
    try {
      out.write((headerLine + newLine).getBytes());
      out.write((thread.getName() + " " + thread.getThreadGroup()).getBytes());
      for(StackTraceElement element : stack){
        out.write((tab + "at " + element.toString() + newLine).getBytes());
      }
    }
    catch (IOException ioe) {
      System.err.println(
          "IOException encountered while trying to write "
              + "StackTraceElement data to provided OutputStream.\n"
              + ioe.getMessage());
    }
  }

  public static void forceInterrupt(Thread thread) {
    if (thread != null) {
      try {
        System.out.println("Forcing interrupt on: " + thread.toString());
        StackTraceElement[] entry = thread.getStackTrace();
        thread.interrupt();
        System.out.println("thead interrupted... " + thread);
        printThreadStack(thread, entry, System.out);
      } catch (SecurityException se) {
        System.out.println("Couldn't interrupt thead " + thread);
      }
    }
  }
}
