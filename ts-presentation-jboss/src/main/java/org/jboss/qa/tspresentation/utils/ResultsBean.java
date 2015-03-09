package org.jboss.qa.tspresentation.utils;

import java.util.LinkedList;

import javax.ejb.Singleton;
import javax.transaction.Transaction;

@Singleton
public class ResultsBean {
    private LinkedList<TxnDTO> txnStatusAtInvoke = new LinkedList<TxnDTO>();
    private LinkedList<TxnDTO> txnStatusAtExit = new LinkedList<TxnDTO>();

    public void clear() {
        txnStatusAtInvoke.clear();
        txnStatusAtExit.clear();
    }

    public LinkedList<TxnDTO> getTxnStatusAtInvoke() {
        return txnStatusAtInvoke;
    }
    public void addTxnStatusAtInvoke(final Transaction txn) {
        this.txnStatusAtInvoke.add(new TxnDTO(txn));
    }
    public LinkedList<TxnDTO> getTxnStatusAtExit() {
        return txnStatusAtExit;
    }
    public void addTxnStatusAtExit(final Transaction txn) {
        this.txnStatusAtExit.add(new TxnDTO(txn));
    }
}
