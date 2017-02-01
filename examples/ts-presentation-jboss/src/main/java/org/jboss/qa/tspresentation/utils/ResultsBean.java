package org.jboss.qa.tspresentation.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.ejb.Singleton;
import javax.transaction.Transaction;

@Singleton
public class ResultsBean {
    private LinkedList<TxnDTO> txnStatusAtInvoke = new LinkedList<TxnDTO>();
    private LinkedList<TxnDTO> txnStatusAtExit = new LinkedList<TxnDTO>();
    private LinkedList<TxnDTO> txnStatusMeanwhile = new LinkedList<TxnDTO>();

    private Map<String, Object> storage = new HashMap<String, Object>();

    public void clear() {
        txnStatusAtInvoke.clear();
        txnStatusAtExit.clear();
        txnStatusMeanwhile.clear();
        storage.clear();
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
    public LinkedList<TxnDTO> getTxnStatusMeanwhile() {
        return txnStatusMeanwhile;
    }
    public void addTxnStatusMeanwhile(final Transaction txn) {
        this.txnStatusMeanwhile.add(new TxnDTO(txn));
    }

    public Map<String, Object> getStorage() {
        return storage;
    }

    public Object getStorageValue(final String key) {
        return storage.get(key);
    }

    public void addStorageValue(final String key, final Object value) {
        this.storage.put(key, value);
    }
}
