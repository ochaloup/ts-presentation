package org.jboss.qa.tspresentation.test.ejb;

import javax.ejb.EJB;
import javax.transaction.xa.XAResource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.BeanToCheckJoiningBranchQualifier;
import org.jboss.qa.tspresentation.ejb.BeanToCheckNotJoiningBranchQualifier;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(Arquillian.class)
public class JoinBranchCheckTest {
    private static final Logger log = LoggerFactory.getLogger(JoinBranchCheckTest.class);
    private static final String DEPLOYMENT = "branch-qualifier-join-check";

    @EJB
    private BeanToCheckJoiningBranchQualifier beanOnePhase;

    @EJB
    private BeanToCheckNotJoiningBranchQualifier beanNotJoin;

    @EJB
    private ResultsBean results;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addClass(BeanToCheckJoiningBranchQualifier.class)
                .addClass(BeanToCheckNotJoiningBranchQualifier.class)
                .addAsManifestResource("beans.xml");
        return jar;
    }

    @Before
    public void setUp() {
        results.clear();
    }

    /**
     * Two datasources aiming to the same datasource instance.
     * JCA will join it and there will be used the same connection and the same XAResource.
     * This means that TM will use one phase commit optimization.
     */
    @Test
    public void runInsertionOnePhase() {
        log.info("test runInsertionOnePhase");
        beanOnePhase.createTable(BeanToCheckJoiningBranchQualifier.TABLE_NAME);
        beanOnePhase.runInsertion();
    }

    /**
     * Two different databases in PostgreSQL.
     * {@link com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple#isNewRM(XAResource)} is called but
     * it returns false and the two branches are created.
     *
     * This could be different for other database vendors but jdbc driver of PostgreSQL does like:
     * public boolean isSameRM(XAResource xares) throws XAException {
     *   return xares == this;
     * }
     */
    @Test
    public void runInsertionNotJoin() {
        log.info("test runInsertionNotJoin");
        beanNotJoin.createTable(BeanToCheckJoiningBranchQualifier.TABLE_NAME);
        beanNotJoin.runInsertion();
    }
}
