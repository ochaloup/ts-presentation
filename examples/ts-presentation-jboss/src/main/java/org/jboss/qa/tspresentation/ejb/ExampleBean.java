package org.jboss.qa.tspresentation.ejb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.jboss.qa.tspresentation.cdi.ExceptionWorkerCdiBean;



@Stateless
public class ExampleBean {

    @Resource(lookup = "java:jboss/datasource-test")
    DataSource datasource;

    @Inject
    ExceptionWorkerCdiBean cdi;

    public int doInsert() {
    	return cdi.doNotRollback();
    }
}
