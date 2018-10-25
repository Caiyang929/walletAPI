package io.baic.cli;

public class Config {
    private String url;
    private String walletUrl;
    private String walletName = "default";
    private String walletPassword ;

    public Config(String url, String wallet_url) {
        this.url = url;
        this.walletUrl = wallet_url;
    }

    public Config(String hostname, int port, String walletHostName, int walletPort) {
        this.url = "http://" + hostname + ":" + Integer.toString(port);
        this.walletUrl = "http://" + walletHostName + ":" + Integer.toString(walletPort);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getWalletUrl() {
        return walletUrl;
    }

    public void setWalletUrl(String walletUrl) {
        this.walletUrl = walletUrl;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getWalletPassword() {
        return walletPassword;
    }

    public void setWalletPassword(String walletPassword) {
        this.walletPassword = walletPassword;
    }
}
