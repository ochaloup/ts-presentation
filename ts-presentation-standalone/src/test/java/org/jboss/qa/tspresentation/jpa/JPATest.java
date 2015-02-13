package org.jboss.qa.tspresentation.jpa;

import java.sql.SQLException;

import org.jboss.qa.tspresentation.jdbc.JDBCDriver;
import org.junit.BeforeClass;
import org.junit.Test;

public class JPATest {

    @BeforeClass
    public static void registerDriver() throws SQLException {
        JDBCDriver.registerDriver();
    }

    @Test
    public void go() {
        JPAProvider jpa = new JPAProvider();
        jpa.doWork();
    }
}
