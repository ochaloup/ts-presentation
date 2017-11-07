package io.narayana.ejb;

import java.util.Hashtable;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;
 
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class RemoteEJBClient {
     
    @Resource
    private UserTransaction ut;
    
    public void call() throws Throwable {

        Hashtable p = new Hashtable();
        p.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        p.put("jboss.naming.client.ejb.context",true);
             
        Context    context = new InitialContext(p);

        Service service =  (Service) context
                .lookup("ejb:/wfly-server/ServiceEJB!io.narayana.ejb.Service");
         
        try {
            ut.begin();
            service.exec();
            ut.commit();
        } catch (Throwable exc) {
            //exc.printStackTrace();
            
            System.out.println("##########################################################");
            
            System.out.println("getMessage():" + exc.getMessage());
            System.out.println("getClass():" + exc.getClass());
            
            if (exc != null)
                System.out.println("getCause():" + exc.getCause());
            
            if (exc.getCause() != null)
            System.out.println("getCause().getSuppressed():" + exc.getCause().getSuppressed());
            
            if (exc.getCause() != null && exc.getCause().getSuppressed()[0].getCause() != null)
            System.out.println("getCause().getSuppressed().getCause():" + exc.getCause().getSuppressed()[0].getCause());
            System.out.println("##########################################################");
            
            // There's no active transaction at this point!
            //ut.rollback();
        }

    }
 
}
