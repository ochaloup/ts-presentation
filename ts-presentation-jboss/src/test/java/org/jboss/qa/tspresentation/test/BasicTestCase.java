package org.jboss.qa.tspresentation.test;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.bean.TestBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class BasicTestCase {
    
    @Inject TestBean testBean;
    
    @Deployment(name = "basic")
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "basic.jar")
                .addPackage("org.jboss.qa.tspresentation.bean");
        return jar;
    }
    
    @Test
    public void test() {
        testBean.doWork();
    }
}
