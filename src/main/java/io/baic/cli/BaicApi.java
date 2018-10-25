package io.baic.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.baic.cli.exception.BaicException;
import io.baic.cli.internal.Interface;
import io.baic.cli.internal.common.Action;
import io.baic.cli.internal.wallet.WalletManager;
import io.baic.cli.utils.Asset;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class BaicApi {
    private Interface anInterface;
    private WalletManager walletManager;
    private Config config;

    public BaicApi(Config config) {
        anInterface = new Interface(config);
        config = config;
    }

    public JsonObject getInfo() throws BaicException {
        return anInterface.getInfo();
    }

    public void unlock() throws BaicException {
        walletManager.unlock(config.getWalletName(), config.getWalletPassword());
    }

    public void lock() throws BaicException {
        walletManager.lock(config.getWalletName());
    }

    // code : baic.token action: transfer
    public JsonArray getTransactionsByBlockRange(long strartBlock, long endBlock, final String code, final String action_name) throws BaicException {
        JsonArray result = new JsonArray();
        for (long i = strartBlock; i < endBlock; i++) {
            JsonObject block = anInterface.getBlock(i);

            JsonArray trxs = block.get("transactions").getAsJsonArray();
            int trxNum = trxs.size();
            for (int j = 0; j < trxNum; j++) {

                JsonObject transactionContext = trxs.get(j).getAsJsonObject();
                JsonObject trx = transactionContext.get("trx").getAsJsonObject().get("transaction").getAsJsonObject();
                JsonArray actions = trx.get("actions").getAsJsonArray();

                int actionNum = actions.size();
                for (int k = 0; k < actionNum; k++) {
                    JsonObject action = actions.get(k).getAsJsonObject();
                    if (action.get("account").getAsString().compareTo(code) == 0
                            && action.get("name").getAsString().compareTo(action_name) == 0) {
                        JsonObject item = new JsonObject();
                        item.addProperty("block_id", block.get("id").getAsString());
                        item.addProperty("block_num", block.get("block_num").getAsString());
                        item.addProperty("status", transactionContext.get("status").getAsString());
                        item.addProperty("trx_id", transactionContext.get("trx").getAsJsonObject().get("id").getAsString());
                        item.add("data", action.get("data").getAsJsonObject());
                        item.addProperty("gas", 0);
                        result.add(item);
                    }
                }
            }
        }
        return result;
    }

    public Asset getBalance(String accountName, String code, String symbol) throws BaicException, Exception {
        return anInterface.getBalance(accountName, code, symbol);
    }



    public JsonObject createAccount(String creator, String account_name, String ownerKey, String activeKey,
                                Asset buyRam, Asset net, Asset cpu, String privateKey) throws Exception {
        return createAccount(creator, account_name, ownerKey, activeKey,
                buyRam.toString(), net.toString(), cpu.toString(), privateKey);
    }



    public JsonObject createAccount(String creator, String account_name,
                                    String ownerKey, String activeKey,
                                    String buyRam, String net, String cpu,
                                    String privateKey) throws Exception {
        unlock();
        List<Action> actions = new LinkedList<>();
        Action createAccountAction = anInterface.createAccountAction(creator, account_name, ownerKey, activeKey);
        Action buyRamAction = anInterface.createBuyRamAction(creator, account_name, buyRam);
        Action delegatedAction = anInterface.createDelegateAction(creator, account_name, net, cpu, true);

        actions.add(createAccountAction);
        actions.add(buyRamAction);
        actions.add(delegatedAction);

        JsonObject result = anInterface.sendActions(actions, null, 0);
        lock();
        return result;
    }

    public JsonObject createAccount(String creator, String account_name,
                                    String ownerKey, String activeKey,
                                    String buyRam, String net, String cpu) throws Exception {
        return createAccount(creator, account_name, ownerKey, activeKey, buyRam, net, cpu, null);
    }

    public JsonObject createSimpleAccount(String creator) throws BaicException {
        return createSimpleAccount(creator, creator);
    }

    public JsonObject createSimpleAccount(String creator, String author) throws BaicException {
        unlock();
        char[] tmp = new char[12];
        Random random = new Random(System.currentTimeMillis());

        char charmap[] = ".34567abcdefghijklmnopqrstuvwxyz".toCharArray();
        for (int i = 0; i < 12; i ++) {
            tmp[i] = charmap[random.nextInt(11) + 1];
        }

        String[] ownKeypair = anInterface.createKey();
        String[] activeKeypair = anInterface.createKey();
        String ownerKey = ownKeypair[0];
        String activeKey = activeKeypair[0];

        String randomAccount = new String(tmp);
        Action createAccountAction = anInterface.createAccountAction(creator, randomAccount, ownerKey, activeKey);

        anInterface.pushAction("baic", "newaccount", createAccountAction, author);

        JsonObject result = new JsonObject();
        result.addProperty("account_name", randomAccount);
        result.add("owner", new JsonArray());
        result.get("owner").getAsJsonArray().add(ownKeypair[0]);
        result.get("owner").getAsJsonArray().add(ownKeypair[1]);

        result.add("active", new JsonArray());
        result.get("active").getAsJsonArray().add(activeKeypair[0]);
        result.get("active").getAsJsonArray().add(activeKeypair[1]);

        lock();
        return result;
    }

    public JsonObject createRandomAccount(String creator, String buyram, String net, String cpu, String privateKey) throws BaicException, Exception {
        unlock();
        char[] tmp = new char[12];
        Random random = new Random(System.currentTimeMillis());

        char charmap[] = ".34567abcdefghijklmnopqrstuvwxyz".toCharArray();
        for (int i = 0; i < 12; i ++) {
            tmp[i] = charmap[random.nextInt(12) + 1];
        }

        String[] ownKeypair = anInterface.createKey();
        String[] activeKeypair = anInterface.createKey();
        // Interface.
        String ownerKey = ownKeypair[0];
        String activeKey = activeKeypair[0];
        String randomAccount = new String(tmp);
        JsonObject restData = createAccount(creator, randomAccount, ownerKey, activeKey, buyram, net, cpu, privateKey);
        JsonObject result = new JsonObject();
        result.addProperty("transaction_id", restData.get("transaction_id").getAsString());
        result.addProperty("account_name", randomAccount);
        result.add("owner", new JsonArray());
        result.get("owner").getAsJsonArray().add(ownKeypair[0]);
        result.get("owner").getAsJsonArray().add(ownKeypair[1]);

        result.add("active", new JsonArray());
        result.get("active").getAsJsonArray().add(activeKeypair[0]);
        result.get("active").getAsJsonArray().add(activeKeypair[1]);

        lock();
        return result;
    }

    public String transfer(String from, String to, Asset quant, String memo, String author) throws BaicException {
        JsonArray param = new JsonArray();
        param.add(from);
        param.add(to);
        param.add(quant.toString());
        param.add(memo);
        unlock();
        JsonObject element =  anInterface.pushAction(
                "baic.token", "transfer",
                param.toString(), author == null ? from : author);

        String trxId = element.get("transaction_id").getAsString();
        lock();
        return trxId;
    }

    public void importPrivateKey(String key) throws BaicException {
        unlock();
        anInterface.importPrivateKey(key);
        lock();
    }

    public String[] createKey() throws BaicException {
        unlock();
        String[] result = anInterface.createKey();
        lock();
        return result;
    }

    public JsonObject getTransactionStatus(String trxId) throws BaicException, Exception{
        return anInterface.getTransaction(trxId);
    }
}
