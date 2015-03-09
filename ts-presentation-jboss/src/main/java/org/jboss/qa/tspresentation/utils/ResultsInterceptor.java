package org.jboss.qa.tspresentation.utils;

import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

@ResultsLogged
@Interceptor
public class ResultsInterceptor {
    @EJB
    ResultsBean resultBean;

    @AroundInvoke
     public Object interceptWithFillingResultBean(final InvocationContext ctx) throws Exception {
        Context jndiCtx = new InitialContext();
        TransactionManager txManager = (TransactionManager) jndiCtx.lookup("java:jboss/TransactionManager");

        resultBean.addTxnStatusAtInvoke(txManager.getTransaction());
        Object o = ctx.proceed();
        resultBean.addTxnStatusAtExit(txManager.getTransaction());
        return o;
     }
}
