package org.jboss.qa.tspresentation.jpa;

import java.sql.SQLException;

import org.jboss.qa.tspresentation.jdbc.JDBCDriver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JPATest {

    private static ClassLoader originalContextClassloader;

    @BeforeClass
    public static void registerDriver() throws SQLException {
        JDBCDriver.registerDriver();

        /*
         * This is a bit hack as ConnectionProvider of Hibernate does do Class.forName() to get driver class
         * as driver was loaded dynamically so it's not on application classpath.
         * Despite the fact the DriverManagerConnectionProviderImpl is used the DriverManager is used for
         * getting connection but it still loads the class by forName() method
         * When Class.forName() fails then there is a fallback to try to load it from ContextClassLoader
         * and we redefine it here to get it really loaded.
         */
        originalContextClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(JDBCDriver.getDriverClassLoader());
    }

    @AfterClass
    public static void restoreClassloader() {
        // Returning back the loader to not influence other test classes
        Thread.currentThread().setContextClassLoader(originalContextClassloader);
    }

    @Test
    public void go() {

        JPAProvider jpa = new JPAProvider();
        jpa.doWork();
    }
}
