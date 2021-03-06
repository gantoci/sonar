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
package org.sonar.jpa.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.dialect.Dialect;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractDatabaseConnector implements DatabaseConnector {
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractDatabaseConnector.class);

  protected Database database;
  private EntityManagerFactory factory = null;

  protected AbstractDatabaseConnector(Database database) {
    this.database = database;
  }

  public void start() {
    LOG.info("Initializing Hibernate");
    factory = createEntityManagerFactory();

  }

  public void stop() {
    if (factory != null && factory.isOpen()) {
      factory.close();
      factory = null;
    }
    database = null;
  }

  public EntityManagerFactory getEntityManagerFactory() {
    return factory;
  }

  protected EntityManagerFactory createEntityManagerFactory() {
    // other settings are stored into /META-INF/persistence.xml
    Properties props = database.getHibernateProperties();
    logHibernateSettings(props);
    return Persistence.createEntityManagerFactory("sonar", props);
  }

  private void logHibernateSettings(Properties props) {
    if (LOG.isDebugEnabled()) {
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        LOG.debug(entry.getKey() + ": " + entry.getValue());
      }
    }
  }

  public EntityManager createEntityManager() {
    return factory.createEntityManager();
  }

  public final int getDatabaseVersion() {
    throw new UnsupportedOperationException("Moved to " + DatabaseVersion.class.getCanonicalName());
  }

  public final Dialect getDialect() {
    return database.getDialect();
  }
}
