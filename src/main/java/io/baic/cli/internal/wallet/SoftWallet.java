package io.baic.cli.internal.wallet;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.baic.cli.exception.BaicException;
import io.baic.cli.internal.common.Action;
import io.baic.cli.internal.common.SignedTransaction;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SoftWallet {

    public SoftWallet(String walletName) {
        walletName = walletName + "." + walletFileNameExt;
    }

    public SoftWallet(String walletName, String dirPath) {
        walletFilename = dirPath + "/" + walletName + "." + walletFileNameExt;
        _keys = new HashMap<>();
    }


    public void importKey(String wifKey) throws Exception {
        if (isLocked()) {
            throw new BaicException(3120003, "wallet_locked_exception", "Locked wallet");
        }

        ECPrivateKey key = KeyUtil.decodePrivateKey(wifKey);
        if (_keys.containsKey(key)) {
            throw new BaicException(3120008, "key_exist", "Key already exists");
        }

        ECPublicKey publicKey = KeyUtil.cacluatePublicKeyFromPrivateKey(key);
        _keys.put(KeyUtil.encodePublicKey(publicKey), key);
        saveWalletFile();
    }

    public void open() throws IOException {
        loadWalletFile();
    }

    public boolean isLocked() {
        return isLocked;
    }

    public boolean isUnLocked() {
        return !isLocked;
    }

    public SignedTransaction signTransaction(SignedTransaction trx, List<String> keys, String chainId) throws Exception {
        byte[] chainIdBytes = Hex.decode(chainId);

        ByteBuffer buffer = ByteBuffer.allocate(10240);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(chainIdBytes);

        // pack transaction
        Date expirationDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(trx.expiration);
        int expirationSeconds = (int)(expirationDate.getTime() / 1000);
        buffer.putInt(expirationSeconds);
        buffer.putShort((short)(trx.ref_block_num & 0xFFFF));
        buffer.putInt((int)(trx.ref_block_prefix & 0xFFFFFFFF));

        buffer.put(BufferUtil.packUInt(trx.max_net_usage_words));
        buffer.put(BufferUtil.packUInt(trx.max_kcpu_usage));
        buffer.put(BufferUtil.packUInt(trx.delay_sec));

        buffer.putLong(0);
        buffer.putLong(0);

        buffer.put(BufferUtil.packUInt(trx.context_free_actions.size()));
        for (Action action : trx.context_free_actions) {
            BufferUtil.packAaction(buffer, action);
        }

        // Actions
        buffer.put(BufferUtil.packUInt(trx.actions.size()));
        for (Action act : trx.actions) {
            BufferUtil.packAaction(buffer, act);
        }

        // transaction extensions
        buffer.put(BufferUtil.packUInt(0));

        // context free data
        if (trx.context_free_actions.size() > 0) {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            for (String str : trx.context_free_data) {
                sha256.update(str.getBytes());
            }
            byte[] digest = sha256.digest();
            buffer.put(digest, 0, digest.length);
        }
        else {
            buffer.putInt(0);
        }

        byte[] packed = buffer.array();
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest(packed);

        List<String> signatures = new ArrayList<>();
        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            String signature = trySignDiges(digest, key);
            if (signature != null) {
                signatures.add(signature);
                iter.remove();
            }
        }

        trx.signatures = signatures;
        return trx;
    }

    private String trySignDiges(byte[] digest, String key) throws Exception {
        if ( !_keys.containsKey(key)) {
            return null;
        }
        ECPrivateKey privateKey = _keys.get(key);

        ECDSASigner signer = new ECDSASigner();
        ECNamedCurveParameterSpec paramsSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECDomainParameters curve = new ECDomainParameters(paramsSpec.getCurve(), paramsSpec.getG(), paramsSpec.getN(),
                paramsSpec.getH());
        ECPrivateKeyParameters parameters = new ECPrivateKeyParameters(privateKey.getD(), curve);
        signer.init(true, parameters);

        String signatureText = null;
        BigInteger r;
        BigInteger s;

        boolean canonical = false;
        do {

            //https://en.bitcoin.it/wiki/Secp256k1
            BigInteger[] components = signer.generateSignature(digest);

            r = components[0];
            s = components[1];

            // if the s is greater than N/2, then s = N -s
            if (s.compareTo(curve.getN().shiftRight(1)) > 0) {
                s = curve.getN().subtract(s);
            }


            signatureText = KeyUtil.encodeSignature(r, s);

            byte[] sbytes = s.toByteArray();
            byte[] rbytes = r.toByteArray();

            // For canonoical-form:
            // https://en.wikipedia.org/wiki/X.690
            canonical = (rbytes.length == 32) && (sbytes.length == 32);
            canonical &= ((rbytes[0] & 0x80) == 0);
            canonical &= !(rbytes[0] == 0 && ((rbytes[1] & 0x80) == 0));
            canonical &= ((sbytes[0] & 0x80) == 0);
            canonical &= !(sbytes[0] == 0 && ((sbytes[1] & 0x80) == 0));

        } while (!canonical);

        return signatureText;
    }

    public void unlock(String password) throws BaicException {
        try {
            loadWalletFile();

            SHA512Digest digest = new SHA512Digest();

            digest.update(password.getBytes(), 0, password.getBytes().length);
            byte[] passwordBytes = new byte[digest.getDigestSize()];
            digest.doFinal(passwordBytes, 0);

            byte[] keyBytes = Arrays.copyOfRange(passwordBytes, 0, 32);
            byte[] ivBytes = Arrays.copyOfRange(passwordBytes, 32, 48);

            SecretKey pw = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");


            cipher.init(Cipher.DECRYPT_MODE, pw, iv);

            byte[] raw = cipher.doFinal(walletData, 0, walletData.length);

            dupRaw = Arrays.copyOfRange(raw, 0, raw.length);

            checkSum = Arrays.copyOfRange(raw, 0, 64);

            if (Arrays.equals(checkSum, passwordBytes) != true) {
                throw new BaicException(3120005, "wallet_invalid_password_exception", "Invalid wallet password");
            }

            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            ByteBuffer buffer = ByteBuffer.wrap(Arrays.copyOfRange(raw, 64, raw.length));
            int count = BufferUtil.getUint(buffer);

            _keys = new HashMap<>();
            for (int i = 0; i < count; i++) {

                byte[] publicKeyBytes = new byte[33];

                byte which = buffer.get();
                buffer.get(publicKeyBytes, 0, 33);
                byte[] privateKeyBytes = new byte[32];

                which = buffer.get();

                buffer.get(privateKeyBytes, 0, 32);

                ECPrivateKeySpec spec = new ECPrivateKeySpec(new BigInteger(1, privateKeyBytes),
                        ECNamedCurveTable.getParameterSpec("secp256k1"));

                KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
                ECPrivateKey key = (ECPrivateKey) kf.generatePrivate(spec);
                ECPublicKey publicKey = KeyUtil.cacluatePublicKeyFromPrivateKey(key);

                _keys.put(KeyUtil.encodePublicKey(publicKey), key);
            }

            isLocked = false;
        } catch (Exception e) {
            throw new BaicException(3120007, "wallet_unlocked_exception", "Already unlocked");
        }

    }

    public void lock() {
        isLocked = true;
    }

    private void saveWalletFile() throws Exception {
        int size = _keys.size();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        outputStream.write(checkSum, 0, 64);

        byte tmp = 0;
        do {
            byte b = (byte)(size & 0x7F);
            outputStream.write(b);
            size >>= 7;
        } while (size != 0 );

        for (Map.Entry<String, ECPrivateKey> entry : _keys.entrySet()) {
            ECPrivateKey key = entry.getValue();
            ECPublicKey publicKey = KeyUtil.cacluatePublicKeyFromPrivateKey(key);

            byte[] privateKeyBytes = entry.getValue().getD().toByteArray();

            if (privateKeyBytes.length == 33 && privateKeyBytes[0] == 0x00) {
                privateKeyBytes = Arrays.copyOfRange(privateKeyBytes, 1, privateKeyBytes.length);
            }

            byte[] publicKeyBytes = publicKey.getQ().getAffineXCoord().toBigInteger().toByteArray();
            if (publicKeyBytes.length == 33 && publicKeyBytes[0] == 0x00) {
                publicKeyBytes = Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length);
            }

            boolean yIsOld = publicKey.getQ().getAffineYCoord().toBigInteger().getLowestSetBit() == 0;

            outputStream.write((byte)0x00);
            outputStream.write((byte) (yIsOld ? 0x03 : 0x02));
            outputStream.write(publicKeyBytes, 0, publicKeyBytes.length);

            outputStream.write((byte)0x00);
            outputStream.write(privateKeyBytes, 0, privateKeyBytes.length);
            //outputStream.flush();
        }

        outputStream.flush();
        byte[] keyBytes = Arrays.copyOfRange(checkSum, 0, 32);
        byte[] ivBytes = Arrays.copyOfRange(checkSum, 32, 48);

        SecretKey pw = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pw, iv);

        byte[] encryptData = cipher.doFinal(outputStream.toByteArray(), 0, outputStream.toByteArray().length);
        //JsonObject cipher_keys = new JsonObject();
        //cipher_keys.addProperty("cipher_keys", new String(Hex.encode(encryptData)));

        JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(walletFilename)));
        writer.setIndent("  ");
        writer.beginObject();
        writer.name("cipher_keys");
        writer.value(new String(Hex.encode(encryptData)));

        writer.endObject();

        writer.flush();
        writer.close();
    }
    public List<String> listPublicKeys() {
        return new ArrayList<String>(_keys.keySet());
    }


    public String[] createKey(String keyType) throws Exception {
        String[] keypair = new String[2];
        if (keyType == null)
            keyType = defaultKeyType;

        ECPrivateKey key = KeyUtil.createPrivateKey("k1");
        keypair[0] = KeyUtil.encodePublicKey(KeyUtil.cacluatePublicKeyFromPrivateKey(key));
        keypair[1] = KeyUtil.encodePrivateKey(key);

        _keys.put(keypair[0], key);
        saveWalletFile();
        return keypair;
    }

    private void loadWalletFile() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(walletFilename));
        JsonReader jsonReader = new JsonReader(reader);
        jsonReader.setLenient(true);
        String plainText = (new JsonParser()).parse(jsonReader).getAsJsonObject().get("cipher_keys").getAsString();
        walletData = Hex.decode(plainText);
    }

    private String   walletFilename;

    private Map<String, ECPrivateKey> _keys;
    private boolean isLocked = false;
    private SecretKey password;

    private byte[] checkSum;
    final private String walletFileNameExt = ".wallet";
    final private String defaultKeyType = "K1";


    private byte[] dupRaw;

    private byte[] walletData;
}
