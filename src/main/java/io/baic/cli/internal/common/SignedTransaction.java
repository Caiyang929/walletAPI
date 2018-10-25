package io.baic.cli.internal.common;

import java.util.List;

public class SignedTransaction extends Transaction {

    public List<String> signatures;
    public List<String> context_free_data; // for each context-free action, there is an entry here

    public List<String> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<String> signatures) {
        this.signatures = signatures;
    }

    public List<String> getContext_free_data() {
        return context_free_data;
    }

    public void setContext_free_data(List<String> context_free_data) {
        this.context_free_data = context_free_data;
    }
}
