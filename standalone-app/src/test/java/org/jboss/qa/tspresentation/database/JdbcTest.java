package org.jboss.qa.tspresentation.database;

import org.junit.Test;

public class JdbcTest {
    @Test
    public void one() {
        JDBCDriver jdbcDriver = new JDBCDriver();
        jdbcDriver.loadProperties();
    }
}
