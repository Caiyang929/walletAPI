package io.baic.cli.internal.common;

import java.util.List;

public class GetRequiredKeysRequest {
    public Transaction transaction;
    public List<String> available_keys;

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public List<String> getAvailable_keys() {
        return available_keys;
    }

    public void setAvailable_keys(List<String> available_keys) {
        this.available_keys = available_keys;
    }
}
