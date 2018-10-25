package io.baic.cli.internal.common;

import java.util.List;

public class Action{

    public String account;
    public String name;
    public List<PermissionLevel> authorization;
    public String data;

    public Action(List<PermissionLevel> auth, String account_name, String action_name, String data) {
        this.authorization = auth;
        this.account = account_name;
        this.name = action_name;
        this.data = data;
    }


    public void setAuthorization(List<PermissionLevel> authorization) {
        this.authorization = authorization;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
        System.out.println("account " + account.toString());
    }

    public String getName() {
        return name;
    }

    public void setName(String  name) {
        this.name = name;
    }

    public List<PermissionLevel> getAuthorization() {
        return authorization;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
