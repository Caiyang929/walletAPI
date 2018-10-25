package io.baic.cli.internal.common;

public class PermissionLevel {
    public String actor;
    public String permission;

    public PermissionLevel(String actor, String permission) {
        this.actor = actor;
        this.permission = permission;
    }

    public PermissionLevel() {

    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
