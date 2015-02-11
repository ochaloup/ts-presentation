package org.jboss.qa.tspresentation.database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jboss.qa.tspresentation.utils.FileLoader;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JDBCDriver {
    private static final Logger log = LoggerFactory.getLogger(JDBCDriver.class);

    // saying if driver based on project properties was already registered or not
    private static volatile boolean isRegistered = false;

    /**
     * Registering JDBC driver based on information from system properties
     */
    public static void registerDriver() {
        synchronized (JDBCDriver.class) {
            if(isRegistered) {
                return;
            }

            ClassLoader classLoader = FileLoader.loadJar(ProjectProperties.get("db.driver"));
            try {
                Class.forName(ProjectProperties.get("db.jdbc_class"), true, classLoader);
                @SuppressWarnings("unchecked")
                Class<Driver> driverClazz = (Class<Driver>) classLoader.loadClass(ProjectProperties.get("db.jdbc_class"));
                // trouble with class loading - see http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
                DriverDelegation driverDelegation = new DriverDelegation(driverClazz.newInstance());
                DriverManager.registerDriver(driverDelegation);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("Can't load class " + ProjectProperties.get("db.jdbc_class") + " from jarfile " +
                        ProjectProperties.get("db.driver") + " by classloader " + classLoader, cnfe);
            } catch (InstantiationException | IllegalAccessException iae) {
                throw new RuntimeException("Can't instantiate new instance of class '" + ProjectProperties.get("db.jdbc_class") + "'", iae);
            } catch (IllegalStateException ise) {
                throw new RuntimeException("Something illegal happens during our classloading", ise);
            } catch (SQLException sqle) {
                throw new RuntimeException("Can't register driver to driver manager", sqle);
            }
        }
    }

    public Connection getConnection() {
        Connection connection;
        try {
            connection = DriverManager.getConnection(ProjectProperties.get("db.jdbc_url"),
                    ProjectProperties.get("db.username"), ProjectProperties.get("db.password"));
            log.info("Newly created connection: '{}'", connection);
            return connection;
        } catch (SQLException sqle) {
            throw new RuntimeException("Can't get SQL conection", sqle);
        }
    }
}
