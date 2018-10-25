package io.baic.cli.internal.wallet;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import io.baic.cli.internal.common.Base32;
import io.baic.cli.internal.common.Base58;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import javax.crypto.Cipher;

public class KeyUtil {
    private static final String K1_CURVE_NAME = "secp256k1";
    private static final String R1_CURVE_NAME = "secp256r1";

    private static final SecureRandom secureRandom;
    private static final ECNamedCurveParameterSpec ecCurvek1;
    private static final ECNamedCurveParameterSpec ecCurver1;

    static {
        secureRandom = new SecureRandom();
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        ecCurvek1 = ECNamedCurveTable.getParameterSpec(K1_CURVE_NAME);

        ecCurver1 = ECNamedCurveTable.getParameterSpec(R1_CURVE_NAME);
    }

    public static ECPrivateKey createPrivateKey(String keyType) throws Exception {
        byte[] keyBytes = new byte[32];

        secureRandom.nextBytes(keyBytes);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");

            BigInteger s = new BigInteger(1, keyBytes);
            ECPrivateKeySpec privateKeySpec;
            if (keyType.equalsIgnoreCase("r1")) {
                privateKeySpec = new ECPrivateKeySpec(s, ecCurver1);
            } else {
                privateKeySpec = new ECPrivateKeySpec(s, ecCurvek1);
            }
            return (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            throw e;
        }
    }

    public static ECPublicKey cacluatePublicKeyFromPrivateKey(ECPrivateKey key) {
        try {
            KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");

            ECPrivateKeySpec privateKeySpec = kf.getKeySpec(key, ECPrivateKeySpec.class);

            ECPoint Q = ecCurvek1.getG().multiply(privateKeySpec.getD());
            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(Q, privateKeySpec.getParams());
            return (ECPublicKey) kf.generatePublic(publicKeySpec);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getPassword() throws Exception {
        return "PW" + encodePrivateKey(createPrivateKey("K1"));
    }

    public static String encodePrivateKey(ECPrivateKey key) throws Exception {
        byte[] bytes = key.getD().toByteArray();
        if (bytes.length == 33 && bytes[0] == 0x00) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }

        // byte[] checkSum = caculateCheckSum(new byte[]{(byte)0x80}, bytes);
        MessageDigest digest = MessageDigest.getInstance("SHA256");
        digest.update(new byte[]{(byte) 0x80});
        digest.update(bytes);
        byte[] checkSum = digest.digest();
        checkSum = digest.digest(checkSum);

        checkSum = Arrays.copyOfRange(checkSum, 0, 4);

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 0x80).put(bytes).put(checkSum).flip();

        byte[] decodedBytes = new byte[buffer.remaining()];

        buffer.get(decodedBytes);

        boolean legacy = true;
        return Base58.encode(decodedBytes);
    }

    public static String encodePublicKey(ECPublicKey key) {
        byte[] bytes = key.getQ().getAffineXCoord().toBigInteger().toByteArray();
        if (bytes.length == 33 && bytes[0] == 0x00) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }

        boolean yIsOld = key.getQ().getAffineYCoord().toBigInteger().getLowestSetBit() == 0;
        byte[] publicBytes = new byte[33];

        publicBytes[0] = (byte) (yIsOld ? 0x03 : 0x02);

        System.arraycopy(bytes, 0, publicBytes, 1, bytes.length);

        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(publicBytes, 0, publicBytes.length);
        byte[] checksum = new byte[digest.getDigestSize()];
        digest.doFinal(checksum, 0);

        checksum = Arrays.copyOfRange(checksum, 0, 4);

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(publicBytes).put(checksum).flip();

        byte[] decodeBytes = new byte[buffer.remaining()];

        buffer.get(decodeBytes);

        return "BAIC" + Base58.encode(decodeBytes);

    }


    public static ECPrivateKey decodePrivateKey(String str) throws Exception {
        int pivot = str.indexOf('_');
        if (pivot == -1 ) {
            byte[] wif_bytes = Base58.decode(str);
            byte[] keyBytes = Arrays.copyOfRange(wif_bytes, 1, wif_bytes.length - 4);

            MessageDigest digest = MessageDigest.getInstance("SHA256");
            digest.update(new byte[]{(byte)0x80});
            digest.update(keyBytes);
            byte[] checkSum = digest.digest();
            checkSum = digest.digest(checkSum);

            System.out.println(Arrays.equals(checkSum, Arrays.copyOfRange(wif_bytes, wif_bytes.length - 4, wif_bytes.length - 1)));

            ECPrivateKeySpec spec = new ECPrivateKeySpec(new BigInteger(1, keyBytes),
                    ECNamedCurveTable.getParameterSpec("secp256k1"));
            KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
            ECPrivateKey key = (ECPrivateKey)kf.generatePrivate(spec);
            return key;
        }
        return null;
    }

    public static String encodeSignature(final BigInteger r, final BigInteger s) {
        String text = null;

        try {
            byte[] rbytes = r.toByteArray();

            if (rbytes.length == 33 && rbytes[0] == 0x00) {
                rbytes = Arrays.copyOfRange(rbytes, 1, 33);
            }

            byte[] sbytes = s.toByteArray();
            if (sbytes.length == 33 && sbytes[0] == 0x00) {
                sbytes = Arrays.copyOfRange(sbytes, 1, 33);
            }



            MessageDigest digest = MessageDigest.getInstance("RIPEMD160");
            digest.update(new byte[]{0});
            digest.update(rbytes);
            digest.update(sbytes);
            digest.update("K1".getBytes());
            byte[] checkSum = new byte[digest.getDigestLength()];
            checkSum = digest.digest();

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put(((byte)0));
            buffer.put(rbytes);
            buffer.put(sbytes);
            buffer.put(Arrays.copyOfRange(checkSum, 0, 4));
            buffer.flip();

            return Base58.encode(buffer.array());

        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}