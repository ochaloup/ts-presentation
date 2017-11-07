package io.narayana.ejb;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.transaction.xa.XAException;
 
@Stateless
@Remote(Service.class)
public class ServiceEJB implements Service {
 
    @EJB TransactionEJB ejb;
    
    @Override
    public void exec() throws XAException {
            ejb.doTransaction();
   }
    
}
