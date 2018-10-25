package io.baic.cli.internal.common;

public class PermissionLevelWeight {
    private PermissionLevel permission;
    private short           weight;

    public PermissionLevel getPermission() {
        return permission;
    }

    public void setPermission(PermissionLevel permission) {
        this.permission = permission;
    }

    public short getWeight() {
        return weight;
    }

    public void setWeight(short weight) {
        this.weight = weight;
    }
}
