/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Contains and provides class loader extended with the JDBC Driver hosted on the server-side.
 */
public class JdbcDriverHolder {

  private static final Logger LOG = LoggerFactory.getLogger(JdbcDriverHolder.class);

  private TempDirectories tempDirectories;
  private ServerClient serverClient;
  private Settings settings;

  // initialized in start()
  private JdbcDriverClassLoader classLoader = null;

  public JdbcDriverHolder(Settings settings, TempDirectories tempDirectories, ServerClient serverClient) {
    this.tempDirectories = tempDirectories;
    this.serverClient = serverClient;
    this.settings = settings;
  }

  public void start() {
    if (!settings.getBoolean(CoreProperties.DRY_RUN)) {
      LOG.info("Install JDBC driver");
      File jdbcDriver = new File(tempDirectories.getRoot(), "jdbc-driver.jar");
      serverClient.download("/deploy/jdbc-driver.jar", jdbcDriver);
      classLoader = initClassloader(jdbcDriver);
    }
  }

  @VisibleForTesting
  JdbcDriverClassLoader getClassLoader() {
    return classLoader;
  }

  @VisibleForTesting
  static JdbcDriverClassLoader initClassloader(File jdbcDriver) {
    JdbcDriverClassLoader classLoader;
    try {
      ClassLoader parentClassLoader = JdbcDriverHolder.class.getClassLoader();
      classLoader = new JdbcDriverClassLoader(jdbcDriver.toURI().toURL(), parentClassLoader);

    } catch (MalformedURLException e) {
      throw new SonarException("Fail to get URL of : " + jdbcDriver.getAbsolutePath(), e);
    }

    // set as the current context classloader for hibernate, else it does not find the JDBC driver.
    Thread.currentThread().setContextClassLoader(classLoader);
    return classLoader;
  }

  /**
   * This method automatically invoked by PicoContainer and unregisters JDBC drivers, which were forgotten.
   * <p>
   * Dynamically loaded JDBC drivers can not be simply used and this is a well known problem of {@link java.sql.DriverManager},
   * so <a href="http://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-from-an-arbitrary-location">workaround is to use proxy</a>.
   * However DriverManager also contains memory leak, thus not only proxy, but also original driver must be unregistered,
   * otherwise our class loader would be kept in memory.
   * </p>
   * <p>
   * This operation contains unnecessary complexity because:
   * <ul>
   * <li>DriverManager checks the class loader of the calling class. Thus we can't simply ask it about deregistration.</li>
   * <li>We can't use reflection against DriverManager, since it would create a dependency on DriverManager implementation,
   * which can be changed (like it was done - compare Java 1.5 and 1.6).</li>
   * <li>So we use companion - {@link JdbcLeakPrevention}. But we can't just create an instance,
   * since it will be loaded by parent class loader and again will not pass DriverManager's check.
   * So, we load the bytes via our parent class loader, but define the class with this class loader
   * thus JdbcLeakPrevention looks like our class to the DriverManager.</li>
   * </li>
   * </p>
   */
  public void stop() {
    if (classLoader != null) {
      classLoader.clearReferencesJdbc();
      if (Thread.currentThread().getContextClassLoader() == classLoader) {
        Thread.currentThread().setContextClassLoader(classLoader.getParent());
      }
      classLoader = null;
    }
  }

  static class JdbcDriverClassLoader extends URLClassLoader {

    public JdbcDriverClassLoader(URL jdbcDriver, ClassLoader parent) {
      super(new URL[]{jdbcDriver}, parent);
    }

    public void clearReferencesJdbc() {
      InputStream is = getResourceAsStream("org/sonar/batch/bootstrap/JdbcLeakPrevention.class");
      byte[] classBytes = new byte[2048];
      int offset = 0;
      try {
        int read = is.read(classBytes, offset, classBytes.length - offset);
        while (read > -1) {
          offset += read;
          if (offset == classBytes.length) {
            // Buffer full - double size
            byte[] tmp = new byte[classBytes.length * 2];
            System.arraycopy(classBytes, 0, tmp, 0, classBytes.length);
            classBytes = tmp;
          }
          read = is.read(classBytes, offset, classBytes.length - offset);
        }

        Class<?> lpClass = defineClass("org.sonar.batch.bootstrap.JdbcLeakPrevention", classBytes, 0, offset, this.getClass().getProtectionDomain());
        Object obj = lpClass.newInstance();

        List<String> driverNames = (List<String>) obj.getClass().getMethod("unregisterDrivers").invoke(obj);

        for (String name : driverNames) {
          LOG.debug("To prevent a memory leak, the JDBC Driver [{}] has been forcibly deregistered", name);
        }
      } catch (Exception e) {
        LOG.warn("JDBC driver deregistration failed", e);
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException ioe) {
            LOG.warn(ioe.getMessage(), ioe);
          }
        }
      }
    }
  }

}
