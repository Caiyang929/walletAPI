package io.baic.cli.internal;

import com.google.gson.*;
import io.baic.cli.internal.wallet.SoftWallet;
import io.baic.cli.utils.Asset;
import io.baic.cli.exception.BaicException;
import io.baic.cli.Config;
import io.baic.cli.internal.common.Action;
import io.baic.cli.internal.common.GetRequiredKeysRequest;
import io.baic.cli.internal.common.PermissionLevel;
import io.baic.cli.internal.common.SignedTransaction;
import io.baic.cli.internal.io.RestUtil;
import io.baic.cli.utils.SymbolType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

public class Interface {
    private RestUtil util;
    private Config config;

    public static final String abi_json_to_bin = "abi_json_to_bin";
    public static final String get_block = "get_block";
    public static final String get_info = "get_info";
    public static final String get_required_keys = "get_required_keys";
    public static final String push_transaction = "push_transaction";
    public static final String get_code = "get_code";
    public static final String get_currency_balance = "get_currency_balance";

    private static final String get_public_keys = "get_public_keys";
    private static final String sign_transaction = "sign_transaction";
    private static final String import_key = "import_key";

    private String url;
    private String wallet_url;
    private String histroy_url;

    public Interface(Config config) {
        this.url = config.getUrl() + "/v1/chain/";
        this.wallet_url = config.getWalletUrl() + "/v1/wallet/";
        this.histroy_url = config.getUrl() + "/v1/history/";
        this.util = new RestUtil();
        this.config = config;
    }

    public JsonObject getInfo() throws BaicException {
        return (JsonObject) util.call(url, get_info, (String) null);
    }

    public JsonObject getBlock(final long blockNum) throws BaicException {
        JsonObject param = new JsonObject();
        param.addProperty("block_num_or_id", blockNum);

        return (JsonObject) util.call(url, get_block, param);
    }

    public JsonObject getCode(String code) throws Exception {

        JsonObject param = new JsonObject();
        param.addProperty("account_name", code);

        return (JsonObject) util.call(url, get_code, param);
    }

    public Asset getBalance(String accountName, String code, String symbolName) throws BaicException, Exception {
        JsonObject param = new JsonObject();
        param.addProperty("account", accountName);
        param.addProperty("code", code);
        param.addProperty("symbol", symbolName);

        // System.out.println(param.toString());
        JsonArray response = (JsonArray) util.call(url, get_currency_balance, param);
        // System.out.println(response.get(0).getAsString());
        // System.out.println(response);
        if (response.size() == 0) {
            return null;
        }
        return new Asset(response.get(0).getAsString());
    }

    public JsonObject abiJsonToBin(final String contract, final String action, final String data) throws BaicException {

        JsonObject params = new JsonObject();
        params.addProperty("code", contract);
        params.addProperty("action", action);
        params.add("args", (new JsonParser()).parse(data));

        return util.call(url, abi_json_to_bin, params.toString()).getAsJsonObject();
    }

    public JsonObject pushAction(final String code, final String act, final Action action, final String author) throws BaicException {
        List<Action> actions = new LinkedList<>();
        List<String> tx_permission = new LinkedList<>();

        tx_permission.add(author);
        actions.add(action);
        return sendActions(actions, null , 0);

    }

    public JsonObject pushAction(final String contract, final String action_name, final String data, final String author) throws BaicException {

        // get_account_permissions
        List<Action> actions = new LinkedList<>();

        List<String> tx_permission = new LinkedList<>();
        tx_permission.add(author);
        List<PermissionLevel> auth = get_account_permissions(tx_permission);

        actions.add(createAction(auth, contract, action_name, data));

        return sendActions(actions, null, 0);
    }

    public Action createAction(List<PermissionLevel> auth, String code, String act, String data) throws BaicException {

        JsonObject result = abiJsonToBin(code, act, data);
        return new Action(auth, code, act, result.get("binargs").getAsString());
    }

    public JsonObject pushTransaction(SignedTransaction trx, String privateKey, int compressionType) throws BaicException {
        // Set loops, default to last irreversible block if it's not spectified by the user
        JsonObject info = getInfo();
        int last_irreversible_block_num = info.get("last_irreversible_block_num").getAsInt();
        JsonObject refBlock = getBlock(last_irreversible_block_num);
        trx.setReferenceBlock(info);
        JsonArray requiredKeys = new JsonArray();
        if (privateKey == null) {
            JsonObject required_keys = determine_required_keys(trx);

            String timeStr = refBlock.get("timestamp").getAsString();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                trx.setExpiration(dateFormat.parse(timeStr).getTime() + 30 * 1000);
            } catch (ParseException e) {
                throw new BaicException(-1, "unknow", "unknow timestamp format");
            }
            requiredKeys = required_keys.get("required_keys").getAsJsonArray();
        }
        else {
            requiredKeys.add(privateKey);
        }
        JsonObject signedTransaction = signTransaction(trx, requiredKeys, info.get("chain_id").getAsString());

        JsonObject param = new JsonObject();
        param.add("signatures", signedTransaction.get("signatures"));
        param.addProperty("compression", "none");
        param.add("transaction", signedTransaction);

        return (JsonObject) util.call(url, push_transaction, param.toString());
    }

    public JsonObject sendActions(List<Action> actions, String privateKey, int compressionType) throws BaicException {
        SignedTransaction trx = new SignedTransaction();
        trx.setActions(actions);
        return pushTransaction(trx, privateKey, compressionType);
    }


    public List<PermissionLevel> get_account_permissions(List<String> permissions) {
        List<PermissionLevel> result = new LinkedList<>();

        for (int i = 0; i < permissions.size(); i++) {
            String[] pieces = permissions.get(i).split("@");
            PermissionLevel level = new PermissionLevel();
            System.out.println("author : " + pieces[0]);
            level.setActor(pieces[0]);
            if (pieces.length > 1) {
                level.setPermission(pieces[1]);
            } else {
                level.setPermission("active");
            }
            ((LinkedList<PermissionLevel>) result).push(level);
        }
        return result;
    }

    public JsonObject determine_required_keys(final SignedTransaction trx) throws BaicException {
        // List<String> public_keys = (new Gson()).fromJson(io.call(wallet_url, get_public_keys, null), new TypeToken<List<String>>(){}.getType());
        JsonArray public_keys_json = (JsonArray) util.call(wallet_url, get_public_keys, (String) null);
        List<String> publick_keys = new LinkedList<>();
        for (int i = 0; i < public_keys_json.size(); i++) {
            publick_keys.add(((JsonArray) public_keys_json).get(i).getAsString());
        }
        GetRequiredKeysRequest request = new GetRequiredKeysRequest();
        request.setTransaction(trx);
        request.setAvailable_keys(publick_keys);

        String param = (new Gson()).toJson(request, GetRequiredKeysRequest.class);

        return (JsonObject) util.call(url, get_required_keys, param);
    }

    public JsonObject signTransaction(final SignedTransaction trx, JsonArray required_keys, final String chainID) throws BaicException {
        JsonArray param = new JsonArray();
        Gson gson = (new GsonBuilder()).create();
        param.add(gson.toJsonTree(trx, SignedTransaction.class));
        JsonArray keys = new JsonArray();
        keys.add(required_keys);
        param.add(required_keys);
        param.add(gson.toJsonTree(chainID));
        return (JsonObject) util.call(wallet_url, sign_transaction, param.toString());
    }

    public Action createAccountAction(final String creator, final String newaccount, String ownerKey, String activeKey) throws BaicException {
        JsonArray param = new JsonArray() ;
        param.add(creator);
        param.add(newaccount);
        param.add(ownerKey);
        param.add(activeKey);

        List<PermissionLevel> auth = new LinkedList<>();
        auth.add(new PermissionLevel(creator, "active"));

        return createAction(auth, "baic", "newaccount", param.toString());
    }

    public Action createBuyRamAction(final String creator, final String newaccount, final String quantity) throws BaicException, Exception {
        JsonObject param = new JsonObject();
        param.addProperty("buyer", creator);
        param.addProperty("receiver", newaccount);
        param.addProperty("quant", new Asset(quantity, new SymbolType(9, "DUSD")).toString());

        List<PermissionLevel> auth = new LinkedList<>();
        auth.add(new PermissionLevel(creator, "active"));
        return createAction(auth, "baic", "buyram", param.toString());
    }

    public Action createBuyRamActionInSize(final String creator, final String newaccount, final String bytes) throws BaicException {
        JsonObject param = new JsonObject();
        param.addProperty("buyer", creator);
        param.addProperty("receiver", newaccount);
        param.addProperty("bytes", bytes);

        List<PermissionLevel> auth = new LinkedList<>();
        auth.add(new PermissionLevel(creator, "active"));

        return createAction(auth, "baic", "buyram", param.toString());
    }

    public Action createDelegateAction(final String from, final String receiver, String net, String cpu, boolean transfer) throws BaicException, Exception {
        JsonObject param = new JsonObject();
        param.addProperty("from", from);
        param.addProperty("receiver", receiver);
        param.addProperty("stake_net_quantity", new Asset(net, new SymbolType(9, "DUSD")).toString());
        param.addProperty("stake_cpu_quantity", new Asset(cpu, new SymbolType(9, "DUSD")).toString());
        param.addProperty("transfer", transfer);

        List<PermissionLevel> auth = new LinkedList<>();
        auth.add(new PermissionLevel(from, "active"));

        return createAction(auth, "baic", "delegate", param.toString());
    }


    public String[] createKey() throws BaicException {
        String publicKey = "";
        JsonArray param = new JsonArray();
        param.add(config.getWalletName());
        param.add("");
        JsonElement newKey = util.call(wallet_url, "create_key", param.toString());
        if (newKey.isJsonPrimitive()) {
            publicKey = newKey.getAsString();
        }

        String result[] = new String[2];
        result[0] = publicKey;
        param = new JsonArray();
        param.add(config.getWalletName());
        param.add(config.getWalletPassword());

        JsonArray keyPairs = (JsonArray) util.call(wallet_url, "list_keys", param.toString());

        for (int i = 0; i < keyPairs.size(); i++) {
            JsonArray keypair = keyPairs.get(i).getAsJsonArray();
            String pk = keypair.get(0).getAsString();
            String privateKey = keypair.get(1).getAsString();
            if (pk.equals(publicKey)) {
                result[1] = privateKey;
                return result;
            }
        }
        return null;
    }

    public void importPrivateKey(String key) throws BaicException {
        JsonArray param = new JsonArray();
        param.add(config.getWalletName());
        param.add(key);

        util.call(wallet_url, import_key, param.toString());
    }

    public JsonObject getTransaction(String id) throws BaicException, Exception {
        JsonObject param = new JsonObject();
        param.addProperty("id", id);
        JsonObject transaction = (JsonObject) util.call(histroy_url, "get_transaction", param.toString());

        JsonObject result = new JsonObject();
        // System.out.println(transaction);
        result.addProperty("trx_id", transaction.get("id").getAsString());
        result.addProperty("block_num", transaction.get("block_num").getAsString());
        result.addProperty("status", transaction.get("trx")
                .getAsJsonObject().get("receipt").getAsJsonObject()
                .get("status").getAsString()
        );

        JsonObject action = transaction.get("trx").getAsJsonObject()
                .get("trx").getAsJsonObject().get("actions").getAsJsonArray().get(0).getAsJsonObject();

        JsonArray traces = transaction.get("traces").getAsJsonArray();
        int total = traces.size();
        // the last 7 trace are the gas action receipt
        Asset totalGas = new Asset(0, new SymbolType(9, "DUSD"));
        if (total > 7) {
            JsonObject gasAction = traces.get(total - 7).getAsJsonObject();

            String code = gasAction.get("act").getAsJsonObject().get("account").getAsString();
            String name = gasAction.get("act").getAsJsonObject().get("name").getAsString();

            if (code.equals("baic") && name.equals("gas")) {
                // compute the gas
                JsonArray inlineTrace = gasAction.get("inline_traces").getAsJsonArray();
                // first gas transfer
                String toProducer = inlineTrace.get(0).getAsJsonObject()
                                                   .get("act").getAsJsonObject()
                                                   .get("data").getAsJsonObject()
                                                   .get("quantity").getAsString();

                String toPool = inlineTrace.get(1).getAsJsonObject()
                                                  .get("act").getAsJsonObject()
                                                  .get("data").getAsJsonObject()
                                                  .get("quantity").getAsString();
                totalGas = (new Asset(toPool)).add(new Asset(toProducer));
                /*
                System.out.println("pool   " + (new Asset(toPool)).toString());
                System.out.println("prod   " + (new Asset(toProducer)).toString());
                System.out.println("gas    " + totalGas.toString());
                */
            }
        }
        //JsonObject  gasAction = action.get("trx").getAsJsonObject()
        //.get("receipt").getAsJsonObject().get("gas_action").getAsJsonObject().get("data").getAsJsonObject();

        /* compute the gas */
        result.addProperty("gas", totalGas.toString());
        result.addProperty("account", action.get("account").getAsString());
        result.addProperty("name", action.get("name").getAsString());
        result.add("data", action.get("data").getAsJsonObject());

        return result;
    }
}
