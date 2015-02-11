package org.jboss.qa.tspresentation.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcTest {
    private static final Logger log = LoggerFactory.getLogger(JdbcTest.class);

    private JDBCDriver jdbcDriver = new JDBCDriver();

    private static final String TABLE_NAME = "test";
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (id int, text varchar(255))";
    private static final String DROP_TABLE = "DROP TABLE " + TABLE_NAME;
    private static final String INSERT = "INSERT INTO " + TABLE_NAME + " VALUES (?, ?)";
    private static final String SELECT = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";

    @BeforeClass
    public static void loadDriver() throws SQLException {
        JDBCDriver jdbcDriver = new JDBCDriver();
        jdbcDriver.registerDriver();
        try (Connection conn = jdbcDriver.getConnection()) {
            Statement st = conn.createStatement();
            st.executeUpdate(CREATE_TABLE);
        }
    }

    @AfterClass
    public static void dropTable() throws SQLException {
        JDBCDriver jdbcDriver = new JDBCDriver();
        try (Connection conn = jdbcDriver.getConnection()) {
            Statement st = conn.createStatement();
            st.executeUpdate(DROP_TABLE);
        }
    }

    @Test
    public void autocommitTrue() throws SQLException {
        int id = 1; // id to be set
        String text = "text"; // text to be set

        try (Connection conn = jdbcDriver.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(INSERT);
            // conn.setAutoCommit(true); // this is by default
            ps.setInt(1, id);
            ps.setString(2, text);
            ps.executeUpdate();

            // not commited - null expected
            Assert.assertEquals(text, selectById(id));
            try {
                conn.commit();
                Assert.fail("Exception is expected as autoCommit is enabled");
            } catch (Exception e) {
                // this is expected as autoCommit is enabled so commit is not possible
            }
        }
    }

    @Test
    public void autocommitFalse() throws SQLException {
        int id = 1; // id to be set
        String text = "text"; // text to be set

        try (Connection conn = jdbcDriver.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(INSERT);
            ps.setInt(1, id);
            ps.setString(2, text);
            ps.executeUpdate();

            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);
            // not commited - null expected
            Assert.assertNull(selectById(id));
            // commit provided
            conn.commit();
            // commited - text is in db
            Assert.assertEquals(text, selectById(id));
        }
    }

    public String selectById(final int id) throws SQLException {
        try (Connection conn = jdbcDriver.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(SELECT);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                String text = rs.getString("text");
                log.debug("Result of the query is {}", text);
                return text;
            } else {
                return null;
            }
        }
    }
}
