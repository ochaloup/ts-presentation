package org.jboss.qa.tspresentation.test.ejb;


import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.failconnection.CallerBean;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Used for being able to check that container is started and that arquillian extension
 * was triggered.
 */
@RunWith(Arquillian.class)
public class OnDatasourceConnectionFailTestCase {
    private static final String DEPLOYMENT = "on-connection-fail";

    @EJB CallerBean ejbBean;

    @EJB ResultsBean results;

    @PersistenceContext EntityManager em;

    TransactionManager txManager;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addPackage("org.jboss.qa.tspresentation.ejb.failconnection")
                .addClass(JBossTestEntity.class)
                .addAsManifestResource("beans.xml")
                .addAsManifestResource("jta-ds-persistence.xml", "persistence.xml");
        return jar;
    }

    @Before
    public void setUp() throws Exception {
        Context jndiCtx = new InitialContext();
        txManager = (TransactionManager) jndiCtx.lookup("java:jboss/TransactionManager");
        results.clear();

        if(txManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
            txManager.rollback();
        }

    }

    @Test(expected = EJBException.class)
    public void callRethrow() throws Exception {
        ejbBean.callRethrower();
    }

    @Test(expected = EJBException.class)
    public void callCatch() throws Exception {
        ejbBean.callCatch();
    }

    @Test
    public void callRequiresNew() throws Exception {
        ejbBean.callRequiresNew();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        int id = (Integer) results.getStorageValue("id");
        JBossTestEntity entity = em.find(JBossTestEntity.class, id);
        Assert.assertEquals(id, entity.getId());
    }
}