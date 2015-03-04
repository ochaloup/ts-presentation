package org.jboss.qa.tspresentation.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.qa.tspresentation.jdbc.JdbcUtil.*;

/**
 * If I'm checking the PosgreSQL log when running tests I'm getting tons of
 * SET extra_float_digits = 3
 * it's seemed to be run for each new connection. See http://dba.stackexchange.com/questions/31108/set-extra-float-digits-3
 */
public class JdbcTest {
    private static final Logger log = LoggerFactory.getLogger(JdbcTest.class);

    private int id = 1;
    private String text = "testing text";
    private String newText = "different " + text;

    @BeforeClass
    public static void registerDriver() throws SQLException {
        JdbcDriver.registerDriver();
        runSQL(CREATE_TABLE);
    }

    @AfterClass
    public static void dropTable() throws SQLException {
        runSQL(DROP_TABLE);
    }

    @After
    public void cleanTable() throws SQLException {
        runSQL(DELETE_TABLE);
    }

    /**
     * Base usage
     */
    @Test
    public void howTo() throws SQLException {
        Connection connection = null;
        try {
            connection = JdbcDriver.getConnection();
            connection.setAutoCommit(false);

            PreparedStatement ps = connection.prepareStatement("INSERT INTO " + TABLE_NAME + " VALUES (?, ?)");
            ps.setInt(1, id);
            ps.setString(2, text);
            ps.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            // rollback method could thorw the SQLException as well
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException exceptionIgnore) {
                    // ignore
                }
            }
            throw e; // rethrow exception cause to not supress it
        } finally {
            // Closing should happen in finally.
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException exceptionIgnore) {
                    // ignore
                }
            }
        }
    }

    /**
     * Transaction is commited immediatelly after each statement execution.
     */
    @Test
    public void autocommitTrue() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            PreparedStatement ps = getInsert(conn, id, text);
            // conn.setAutoCommit(true); // this is by default
            ps.executeUpdate();

            ps = getInsert(conn, id + 1, text);
            ps.executeUpdate(); // execute() is possible here as well, not so executeQuery()

            // commit expected as it's autocommiting
            Assert.assertEquals(text, selectById(id));
            Assert.assertEquals(text, selectById(id + 1));
            try {
                conn.commit();
                Assert.fail("Exception is expected as autoCommit is enabled");
            } catch (Exception e) {
                // this is expected as autoCommit is enabled so commit is not possible
            }
        }
    }

    /**
     * Transaction is commited when Connection.commit is called
     */
    @Test
    public void autocommitFalse() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            PreparedStatement ps = getInsert(conn, id, text);

            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);

            // execute
            ps.executeUpdate();

            // execute #2
            ps = getInsert(conn, id + 1, text);
            ps.executeUpdate();

            // not commited - null expected
            Assert.assertNull(selectById(id));
            Assert.assertNull(selectById(id + 1));
            // commit provided
            conn.commit();
            // commited - text is in db
            Assert.assertEquals(text, selectById(id));
        }
    }

    /**
     * Transaction is not commited when Connection.rollback is called
     */
    @Test
    public void autocommitFalseWithRollback() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            PreparedStatement ps = getInsert(conn, id, text);

            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);

            // execute
            ps.executeUpdate();

            // not commited - null expected
            Assert.assertNull(selectById(id));
            // commit provided
            conn.rollback();
            // no data as rollback called
            Assert.assertNull(selectById(id));
        }
    }

    /**
     * When close is called prior of calling commit on connection
     * then rollback is done for PostgresSQL.
     * But for Oracle commit is done probably.
     *
     * According to the javadoc, you should try to either commit or roll back before calling the close method. The results otherwise are implementation-defined.
     * (http://stackoverflow.com/questions/218350/does-java-connection-close-rollback)
     */
    @Test
    public void closingConnectionMeansRollback() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            PreparedStatement ps = getInsert(conn, id, text);

            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);

            // execute
            ps.executeUpdate();

            // not commited - null expected
            Assert.assertNull(selectById(id));
            // commit provided
            conn.close();
            // no data as rollback called
            Assert.assertNull(selectById(id));
        }
    }

    /**
     * Updating the same row inside of transaction.
     * When transaction commits then obviously there will be updated value.
     */
    @Test
    public void doubleChangeInTransaction() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);

            PreparedStatement ps = getInsert(conn, id, text);
            // execute
            ps.executeUpdate();
            // not commited - null expected - checked from the different connection/transaction
            Assert.assertNull(selectById(id));
            // checking from the same connection
            Assert.assertEquals(text, selectById(id, conn));

            // writing a new value to database to the same row
            String differentText = "absolutely different text in comparision with " + text;
            PreparedStatement ps2 = getUpdate(conn, id, differentText);
            ps2.executeUpdate();
            Assert.assertEquals(differentText, selectById(id, conn));

            // commit on the connection itself
            conn.commit();
            // updated data available
            Assert.assertEquals(differentText, selectById(id));
        }
    }

    @Test
    public void commitForAutocommitTrue() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            PreparedStatement ps = getInsert(conn, id, text);
            ps.executeUpdate();
            Assert.assertEquals(text, selectById(id));
            try {
                conn.commit();
            } catch (Exception e) {
                log.info("Got an expected exception {} as no transaction is in run", e);
            }
        }
    }

    /**
     * Define a safepoint as a way how to use nested transaction in scope
     * of the resource manager transaction.
     */
    @Test
    public void safePointCommit() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            PreparedStatement ps = getInsert(conn, id, text);

            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);
            // execute
            ps.executeUpdate();
            // not commited - null expected
            Assert.assertNull(selectById(id));
            // selected from the same transaction
            Assert.assertEquals(text, selectById(id, conn));

            Savepoint savePoint = conn.setSavepoint();
            getUpdate(conn, 1, newText).executeUpdate();
            Assert.assertEquals(newText, selectById(id, conn));
            // release meaning a kind of commit on the nested transaction
            conn.releaseSavepoint(savePoint);

            // commit on the connection itself
            conn.commit();
            // no data as rollback called
            Assert.assertEquals(newText, selectById(id));
        }
    }

    /**
     * Rollbacking safepoint.
     */
    @Test
    public void safePointRollback() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            // get command for data insertion
            PreparedStatement ps = getInsert(conn, 1, text);

            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);
            // execute
            ps.executeUpdate();
            // not commited - null expected
            Assert.assertNull(selectById(id));

            // specifying save point as a point where transaction will be nested
            Savepoint savePoint = conn.setSavepoint();
            getUpdate(conn, 1, newText).executeUpdate();
            conn.rollback(savePoint);

            // commit on the connection itself
            conn.commit();
            // inserted data (NOT updated) as rollback was called
            Assert.assertEquals(text, selectById(id));
        }
    }

    /**
     * Nested transaction inside of other nested transaction.
     */
    @Test
    public void safePointDoubleRollback() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);
            // execute
            getInsert(conn, 1, text).executeUpdate();

            // specifying save point as a point where transaction will be nested
            Savepoint savePoint = conn.setSavepoint();
            getUpdate(conn, 1, newText).executeUpdate();
            // second safepoint
            @SuppressWarnings("unused")
            Savepoint savePoint2 = conn.setSavepoint();
            getUpdate(conn, 1, newText + "a").executeUpdate();
            // from the same transaction we can see the updated text
            Assert.assertEquals(newText + "a", selectById(id, conn));

            // rollbacking both safepoints
            conn.rollback(savePoint);

            // after rollback of safepoints we can see the inserted text
            // from the same transaction
            Assert.assertEquals(text, selectById(id, conn));

            // commit on the connection itself
            conn.commit();
            // inserted data (NOT updated) as rollback was called
            Assert.assertEquals(text, selectById(id));
        }
    }

    /**
     * Rollbacking safepoint.
     */
    @Test
    public void safePointWholeTransactionRollback() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement ps = getInsert(conn, 1, text);

            ps.executeUpdate();
            // not commited - null expected
            Assert.assertNull(selectById(id));

            // specifying save point as a point where transaction will be nested
            Savepoint savePoint = conn.setSavepoint();
            getUpdate(conn, 1, newText).executeUpdate();
            conn.releaseSavepoint(savePoint);

            // still no data visible as transaction was not commited
            Assert.assertNull(selectById(id));

            // rollback on the connection itself
            conn.rollback();

            // no data as transaction was rollbacked
            Assert.assertNull(selectById(id));
        }
    }

    /**
     * Using batch of prepare statement.
     * (Nothing much with transaction management :)
     */
    @Test
    public void runBatch() throws SQLException {
        int id2 = 2;

        try (Connection conn = JdbcDriver.getConnection()) {
            // saying that I will manage transaction on the connection
            // it's needed for batch as well - otherwise each batch item
            // will be executed in its own transaction
            conn.setAutoCommit(false);

            PreparedStatement ps = getInsert(conn, 1, text);
            ps.addBatch();

            ps.setInt(1, id2);
            ps.setString(2, newText);
            ps.addBatch();

            ps.executeBatch();

            Assert.assertNull(selectById(id));
            Assert.assertNull(selectById(id2));

            conn.commit();

            Assert.assertEquals(text, selectById(id));
            Assert.assertEquals(newText, selectById(id2));
        }
    }

    /**
     * DDL commands behaves differently for different vendors.
     * https://wiki.postgresql.org/wiki/Transactional_DDL_in_PostgreSQL:_A_Competitive_Analysis
     *
     * For PostgreSQL the DDL commands are transactional but e.g. for Oracle they are not
     * and DDL commands means commits the previous transaction as first step.
     */
    @Test
    @Ignore
    public void runDDL() throws SQLException {
        String tableName = "RUN_DDL_TEST_TABLE";
        String selectTableSQL = String.format(SELECT_PATTERN, tableName);
        String dropTableSQL = String.format(DROP_TABLE_PATTERN, tableName);

        try {
            // no table should exist
            runSQL(selectTableSQL);
            log.debug("Table {} does exist - dropping it", tableName);
            runSQL(dropTableSQL);
        } catch (Exception e) {
            // ignore - expecting that table does not exist
        }

        try (Connection conn = JdbcDriver.getConnection()) {
            // starting transaction
            conn.setAutoCommit(false);
            // doing ddl command
            String createTableSQL = String.format(CREATE_TABLE_PATTERN, tableName);
            Statement st = conn.createStatement();
            st.execute(createTableSQL);

            // table was created but as DDL is not part of transaction
            // we are able to see it from scope outside of this transaction
            runSQL(selectTableSQL);

            conn.commit();
        } catch (Exception e) {
            log.error("ERROR: {}", e);
            Assert.fail("Can't finish the test. Cause: " + e.getMessage());
        } finally {
            try (Connection conn = JdbcDriver.getConnection()) {
                Statement st = conn.createStatement();
                st.executeQuery(dropTableSQL);
            } catch (Exception e) {
                log.warn("Can't drop table {}: {}", tableName, e);
            }
        }
    }

    /**
     * PostgreSQL e.g. not support isolation level 0:
     * org.postgresql.util.PSQLException: Transaction isolation level 0 not supported.
     */
    @Test
    @Ignore
    public void transactionIsolationTRANSACTION_NONE() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_NONE);
            PreparedStatement ps = getInsert(conn, id, text);
            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);
            // execute
            ps.executeUpdate();
            // not commited - but running without transaction here
            Assert.assertEquals(text, selectById(id));
            // commit provided
            conn.commit();
        }
    }

    /**
     * In PostgreSQL READ UNCOMMITTED is treated as READ COMMITTED, while REPEATABLE READ is treated as SERIALIZABLE.
     */
    @Test
    @Ignore
    public void transactionIsolationTRANSACTION_READ_UNCOMMITED() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            PreparedStatement ps = getInsert(conn, id, text);
            // saying that I will manage transaction on the connection
            conn.setAutoCommit(false);
            // execute
            ps.executeUpdate();
            // not commited - but running without transaction here
            Assert.assertEquals(text, selectById(id));
            // commit provided
            conn.commit();
        }
    }

    @Test
    public void transactionIsolationTRANSACTION_READ_COMMITED() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            getInsert(conn, id, text).executeUpdate();
        }

        try (Connection conn = JdbcDriver.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);

            // not commited - but running without transaction here
            String takenText = selectById(id, conn);

            try (Connection connAnother = JdbcDriver.getConnection()) {
                connAnother.setAutoCommit(false);
                // update in different transaction
                getUpdate(connAnother, 1, newText).executeUpdate();
                connAnother.commit();
            }

            // as non-repeatable reads are permitted then the texts will be different
            // as meanwhile update was executed
            Assert.assertNotEquals(takenText, selectById(id, conn));
            Assert.assertEquals(newText, selectById(id, conn));
            Assert.assertEquals(newText, selectById(id));
            // commit provided
            conn.commit();
        }
    }

    @Test
    public void transactionIsolationTRANSACTION_REPEATABLE_READ() throws SQLException {
        try (Connection conn = JdbcDriver.getConnection()) {
            getInsert(conn, id, text).executeUpdate();
        }

        try (Connection conn = JdbcDriver.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            conn.setAutoCommit(false);

            // not commited - but running without transaction here
            String takenText = selectById(id, conn);

            try (Connection connAnother = JdbcDriver.getConnection()) {
                connAnother.setAutoCommit(false);
                // update in different transaction
                getUpdate(connAnother, 1, newText).executeUpdate();
                connAnother.commit();
            }

            // non-repeatable reads are not permitted so we will see the text as at time of first read
            Assert.assertEquals(takenText, selectById(id, conn));
            Assert.assertEquals(newText, selectById(id));
            // commit provided
            conn.commit();
        }
    }
}
