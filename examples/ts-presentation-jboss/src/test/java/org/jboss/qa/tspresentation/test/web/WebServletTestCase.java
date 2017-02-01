package org.jboss.qa.tspresentation.test.web;


import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for being able to check that container is started and that arquillian extension
 * was triggered.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebServletTestCase {
    private static final Logger log = LoggerFactory.getLogger(WebServletTestCase.class);
    private static final String DEPLOYMENT = "web-fail-deployment";

    @ArquillianResource
    protected URL baseUrl;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war")
                .addPackage("org.jboss.qa.tspresentation.utils").addClass(ProjectProperties.class)
                .addPackage("org.jboss.qa.tspresentation.web")
                .addClass(JBossTestEntity.class)
                .addAsWebInfResource("jta-ds-persistence.xml", "classes/META-INF/persistence.xml");
        return war;
    }

    @Test
    public void startTransactionOnlyWithResourceAnnotation() throws Exception {
        URL url = WebUtils.concatUrl(baseUrl, "start-only-resource");
        String responseString = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString);

        Assert.assertEquals("started", responseString);

        // TODO: instead of not_supported exception is just put error message to log
        // JBAS010152: APPLICATION ERROR: transaction still active in request with status 0
        // not sure if exception not should be thrown
        responseString = WebUtils.readUrl(url);
        Assert.assertEquals("started", responseString);
    }

    @Test
    public void startTransactionOnlyWithInjectAnnotation() throws Exception {
        URL url = WebUtils.concatUrl(baseUrl, "start-only-inject");
        String responseString = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString);

        Assert.assertEquals("started-inject", responseString);

        // TODO: instead of not_supported exception is just put error message to log
        // JBAS010152: APPLICATION ERROR: transaction still active in request with status 0
        // not sure if exception not should be thrown
        responseString = WebUtils.readUrl(url);
        Assert.assertEquals("started-inject", responseString);
    }

    @Test
    public void statefulBeanWithEJBAnnotation() throws Exception {
        URL url = WebUtils.concatUrl(baseUrl, "stateful-ejb");

        String responseString1 = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString1);

        String responseString2 = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString2);

        Assert.assertEquals("@EJB *is not* session scoped so the both beans should be the same",
                responseString1, responseString2);
    }

    @Test
    public void statefulBeanWithInjectAnnotation() throws Exception {
        URL url = WebUtils.concatUrl(baseUrl, "stateful-inject");

        String responseString1 = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString1);

        String responseString2 = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString2);

        Assert.assertNotEquals("@Inject *is* session scoped so the both beans should *not* be the same",
                responseString1, responseString2);
    }

    @Test
    public void transactionRunInParallel() throws Exception {
        final URL url = WebUtils.concatUrl(baseUrl, "transaction-run");

        ExecutorService pool = Executors.newFixedThreadPool(3);
        Set<Future<String>> set = new HashSet<Future<String>>();
        for (int i=1; i<=3; i++) {
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return WebUtils.readUrl(url);
                }
            };
            Future<String> future = pool.submit(callable);
            Thread.sleep(1400);
            set.add(future);
          }

        for(Future<String> future: set) {
            String responseString = future.get();
            log.info("From {} got '{}'", url, responseString);
            Assert.assertEquals("done", responseString);
        }
    }

    /**
     * WebServlet can't be transactional defined by CDI annotation (it seems).
     */
    @Test(expected = RuntimeException.class)
    public void cdiTransactional() throws Exception {
        URL url = WebUtils.concatUrl(baseUrl, "transactional");

        String responseString1 = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString1);
    }

    @Test
    public void extendedJpaWithStatefulBean() throws Exception {
        URL url = WebUtils.concatUrl(baseUrl, "jpa?extended");
        String responseString = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString);
        Assert.assertEquals("my-name", responseString);

        url = WebUtils.concatUrl(baseUrl, "jpa?extended&change");
        responseString = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString);
        Assert.assertEquals("my-new-name", responseString);
    }

    @Test
    public void jpaWithStatefulBean() throws Exception {
        URL url = WebUtils.concatUrl(baseUrl, "jpa");
        String responseString = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString);
        Assert.assertEquals("my-name", responseString);

        url = WebUtils.concatUrl(baseUrl, "jpa?change");
        responseString = WebUtils.readUrl(url);
        log.info("From {} got '{}'", url, responseString);
        Assert.assertEquals("my-new-name", responseString);
    }
}