package org.jboss.qa.tspresentation.utils;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

public class TxnDTO {
    private int code;
    private String status;
    private int hashCode;

    public TxnDTO(final Transaction txn) {
        if(txn == null) {
            this.code = Status.STATUS_NO_TRANSACTION;
            this.hashCode = -1;
        } else {
            try {
                this.code = txn.getStatus();
                this.hashCode = txn.hashCode();
            } catch (SystemException se) {
                throw new RuntimeException(se);
            }
        }
        this.status = convertStatus(code);
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public int getHashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return hashCode + "{" + status + ":" + code + "}";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
        result = prime * result + hashCode;
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof TxnDTO))
            return false;
        TxnDTO other = (TxnDTO) obj;
        if (code != other.code)
            return false;
        if (hashCode != other.hashCode)
            return false;
        if (status == null) {
            if (other.status != null)
                return false;
        } else if (!status.equals(other.status))
            return false;
        return true;
    }

    private String convertStatus(final int statusCode) {
        switch (statusCode) {
        case Status.STATUS_ACTIVE:
            return "STATUS_ACTIVE";
        case Status.STATUS_MARKED_ROLLBACK:
            return "STATUS_MARKED_ROLLBACK";
        case Status.STATUS_COMMITTED:
            return "STATUS_COMMITTED";
        case Status.STATUS_COMMITTING:
            return "STATUS_COMMITTING";
        case Status.STATUS_NO_TRANSACTION:
            return "STATUS_NO_TRANSACTION";
        case Status.STATUS_PREPARED:
            return "STATUS_PREPARED";
        case Status.STATUS_PREPARING:
            return "STATUS_PREPARING";
        case Status.STATUS_ROLLEDBACK:
            return "STATUS_ROLLEDBACK";
        case Status.STATUS_ROLLING_BACK:
            return "STATUS_ROLLING_BACK";
        case Status.STATUS_UNKNOWN:
            return "STATUS_UNKNOWN";
        default:
            throw new IllegalArgumentException("Transaction can't have status of code " + status);
        }
    }
}
