package org.jboss.qa.tspresentation.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcUtil {
    private static final Logger log = LoggerFactory.getLogger(JdbcUtil.class);

    public static final String TABLE_NAME = "test";
    public static final String TEXT_COLUMN_NAME = "text";
    public static final String CREATE_TABLE_PATTERN = "CREATE TABLE %s (id int, " + TEXT_COLUMN_NAME + " varchar(255))";
    public static final String CREATE_TABLE = String.format(CREATE_TABLE_PATTERN, TABLE_NAME);
    public static final String DROP_TABLE_PATTERN = "DROP TABLE %s";
    public static final String DROP_TABLE =  String.format(DROP_TABLE_PATTERN, TABLE_NAME);
    public static final String DELETE_TABLE_PATTERN = "DELETE FROM %s";
    public static final String DELETE_TABLE = String.format(DELETE_TABLE_PATTERN, TABLE_NAME);
    public static final String INSERT = "INSERT INTO " + TABLE_NAME + " VALUES (?, ?)";
    public static final String UPDATE = "UPDATE " + TABLE_NAME + " SET id = ?, text = ? WHERE id = ?";
    public static final String SELECT_PATTERN = "SELECT * FROM %s";
    public static final String SELECT_WHERE_PATTERN = " WHERE id = ?";
    // private static final String SELECT_WHERE = String.format(SELECT_PATTERN + SELECT_WHERE_PATTERN, TABLE_NAME);

    public static boolean runSQL(final String sql) throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            Statement st = conn.createStatement();
            return st.execute(sql);
        }
    }

    public static String selectById(final int id, final Connection conn) throws SQLException {
        return selectById(id, TABLE_NAME, conn);
    }

    public static String selectById(final int id, final String tableName, final Connection conn) throws SQLException {
        return selectById(id, tableName, TEXT_COLUMN_NAME, conn);
    }

    public static String selectById(final int id, final String tableName, final String columnName, final Connection conn) throws SQLException {
        String selectClause = String.format(SELECT_PATTERN + SELECT_WHERE_PATTERN, tableName) ;
        PreparedStatement ps = conn.prepareStatement(selectClause);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if(rs.next()) {
            String text = rs.getString(columnName);
            log.debug("Result of the query '{}' is '{}'", ps.toString(), text);
            return text;
        } else {
            return null;
        }
    }

    public static String selectById(final int id) throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            return selectById(id, conn);
        }
    }

    public static PreparedStatement getInsert(final Connection conn, final int prepId, final String prepText) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(INSERT);
        ps.setInt(1, prepId);
        ps.setString(2, prepText);
        return ps;
    }

    public static PreparedStatement getUpdate(final Connection conn, final int prepId, final String prepText) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(UPDATE);
        ps.setInt(1, prepId);
        ps.setString(2, prepText);
        ps.setInt(3, prepId);
        return ps;
    }
}
