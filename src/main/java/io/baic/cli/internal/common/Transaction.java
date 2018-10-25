package io.baic.cli.internal.common;

import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.List;

public class Transaction {

    public String    expiration = "2018-06-07T03:50:08.000"; // time_point_sec
    public int    region;
    public long    ref_block_num;
    public long    ref_block_prefix;
    public int    max_net_usage_words;
    public int    max_kcpu_usage;
    public long   gas_limit;
    public String gas_payer = "";
    public int    delay_sec;
    public List<Action> actions;
    public List<Action> context_free_actions;


    public void setReferenceBlock(JsonObject referenceBlock) {

        setRef_block_num(referenceBlock.get("last_irreverisble_block_ref_num").getAsInt());
        setRef_block_prefix(referenceBlock.get("last_irreversible_block_ref_prefix").getAsLong());
    }

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public void setExpiration(long timeMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(("yyyy-MM-dd'T'HH:mm:ss"));
        this.expiration = dateFormat.format(timeMillis);
    }

    public int getRegion() {
        return region;
    }

    public void setRegion(int region) {
        this.region = region;
    }


    public void setRef_block_num(int ref_block_num) {
        this.ref_block_num = ref_block_num;
    }

    public long getGas_limit() {
        return gas_limit;
    }

    public void setGas_limit(long gas_limit) {
        this.gas_limit = gas_limit;
    }

    public String getGas_payer() {
        return gas_payer;
    }

    public void setGas_payer(String gas_payer) {
        this.gas_payer = gas_payer;
    }

    public long getRef_block_prefix() {
        return ref_block_prefix;
    }

    public void setRef_block_prefix(int ref_block_prefix) {
        this.ref_block_prefix = ref_block_prefix;
    }

    public long getMax_net_usage_words() {
        return max_net_usage_words;
    }

    public void setMax_net_usage_words(int max_net_usage_words) {
        this.max_net_usage_words = max_net_usage_words;
    }

    public int getMax_kcpu_usage() {
        return max_kcpu_usage;
    }

    public void setMax_kcpu_usage(int max_kcpu_usage) {
        this.max_kcpu_usage = max_kcpu_usage;
    }


    public int getDelay_sec() {
        return delay_sec;
    }

    public void setDelay_sec(int delay_sec) {
        this.delay_sec = delay_sec;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public List<Action> getContext_free_actions() {
        return context_free_actions;
    }

    public void setContext_free_actions(List<Action> context_free_actions) {
        this.context_free_actions = context_free_actions;
    }

    public long getRef_block_num() {
        return ref_block_num;
    }

    public void setRef_block_num(long ref_block_num) {
        this.ref_block_num = ref_block_num;
    }

    public void setRef_block_prefix(long ref_block_prefix) {
        this.ref_block_prefix = ref_block_prefix;
    }
}
