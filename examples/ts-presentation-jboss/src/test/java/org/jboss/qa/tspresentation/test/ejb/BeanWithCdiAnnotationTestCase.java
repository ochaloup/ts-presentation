package org.jboss.qa.tspresentation.test.ejb;


import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.BeanWithCdiAnnotation;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.JdbcBean;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class BeanWithCdiAnnotationTestCase {
    private static final String DEPLOYMENT = "ejb-with-cdi-annotation";
    private static final int id = 42;
    private static final String value = "@Transactional";

    @EJB BeanWithCdiAnnotation ejbBean;

    @EJB JdbcBean jdbcBean;

    @Resource
    private UserTransaction utx;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addClass(BeanWithCdiAnnotation.class)
                .addClass(JBossTestEntity.class)
                .addAsManifestResource("beans.xml");
        return jar;
    }

    @Before
    public void setUp() throws Exception {
        jdbcBean.delete(JBossTestEntity.TABLE_NAME);
        if(utx.getStatus() != Status.STATUS_NO_TRANSACTION) {
            utx.rollback();
        }
    }

    @Test
    public void commitAsTransactionNotSupported() throws Exception {
        try {
            ejbBean.transactionNotSupported(id, value);
            Assert.fail("the test method is expected to throw runtime exception to get things rolled back in case");
        } catch (RuntimeException re) {
            // this is expected and it's ok
        }

        String result = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);
        Assert.assertEquals("expecting data was saved as @Transactional attribute defined that there is no transaction happening",
            value, result);
    }
    
    @Test
    public void commitAsRollbackingDifferentTransaction() throws Exception {
        utx.begin();
        ejbBean.transactionRequiresNew(id, value);
        utx.rollback();

        String result = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);
        Assert.assertEquals("expecting data was saved as @Transactional attribute defined new transaction to be created",
           value, result);
    }
}