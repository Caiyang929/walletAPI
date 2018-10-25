package io.baic.cli.internal.wallet;


import io.baic.cli.exception.BaicException;
import io.baic.cli.internal.common.SignedTransaction;

import java.io.IOException;
import java.security.PublicKey;
import java.util.*;

public class WalletManager {
    public void open(String name) throws BaicException {
        if (name == null) {
            name = "default";
        }

        try {
            SoftWallet wallet = wallets.get(name);
            if (wallet == null) {
                wallet = new SoftWallet(name);
                wallet.open();
                wallets.put(name, wallet);
            }
        }
        catch (IOException e) {
            throw new BaicException(3120002, "wallet_nonexistent_exception", "Failed to load the specified wallet: " + name);
        }
    }

    public void unlock(String name, String password) throws BaicException {
        if (name == null) {
            name = "deafult";
        }
        try {
            SoftWallet wallet = wallets.get(name);
            if (wallet == null) {
                wallet = new SoftWallet(name);
                wallet.open();
                wallet.unlock(password);
                wallets.put(name, wallet);
            }
        }
        catch (IOException e) {
            throw new BaicException(3120002, "wallet_nonexistent_exception", "Failed to load the specified wallet: " + name);
        }
    }

    public void lock(String name) throws BaicException {
        if (name == null) {
            name = "deafult";
        }

        SoftWallet wallet = wallets.get(name);
        if (wallet == null) {
            throw new BaicException(3120002, "wallet_nonexistent_exception", "Failed to load the specified wallet: " + name);
        }
        wallet.lock();

    }
    public List<String> list() {
        return listWallets();
    }

    public List<String> listWallets() {
        return new ArrayList<String>(wallets.keySet());
    }

    public List<String> getPublicKeys(String name) throws BaicException {
        if ( name == null ) {
            name = "default";
        }

        if (wallets.containsKey(name)) {
            throw new BaicException(3120006, "No available wallet",  "wallet_not_available_exception");
        }

        List<String> publicKeys = null;

        SoftWallet wallet = wallets.get(name);
        return wallet.listPublicKeys();
    }

    private Map<String, SoftWallet> wallets;
    private String dir;
    private String fileExt;
}
