package io.baic.cli.exception;

public class BaicException extends Exception {
    private int code;
    private String name;
    private String what;

    public BaicException(int errorCode, String name, String what) {
        super(what);
        this.code = errorCode;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWhat() {
        return what;
    }

    public void setWhat(String what) {
        this.what = what;
    }
}
