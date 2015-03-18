package org.jboss.qa.tspresentation.ejb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;



@Stateless
public class ExampleBean {

    @Resource(lookup = "java:jboss/datasource-test")
    DataSource datasource;

    public int doInsert() {
        try(Connection c = datasource.getConnection()) {
            PreparedStatement ps =
                c.prepareStatement("INSERT INTO "
                    + "JBOSS_TEST_ENTITY VALUES (?,?)");
            ps.setInt(1, 1);
            ps.setString(2, "JBoss QE");
            return ps.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }
}
