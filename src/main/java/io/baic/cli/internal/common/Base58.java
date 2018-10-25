package io.baic.cli.internal.common;
import java.util.Arrays;
/**
 * @author yangc
 * @date 2018/10/15
 * @desc
 */
public class Base58 {
    // Bsae58 Coding table
    public static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char ENCODED_ZERO = ALPHABET[0];
    private static final int[] INDEXES = new int[128];
    static {
        Arrays.fill(INDEXES, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEXES[ALPHABET[i]] = i;
        }
    }
    // Base58 Coding
    public static String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }
        // Statistics repair 0
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            ++zeros;
        }
        // copy and edit
        input = Arrays.copyOf(input, input.length);
        // Maximum encoding data length
        char[] encoded = new char[input.length * 2];
        int outputStart = encoded.length;
        // Base58 coding start
        for (int inputStart = zeros; inputStart < input.length; ) {
            encoded[--outputStart] = ALPHABET[divmod(input, inputStart, 256, 58)];
            if (input[inputStart] == 0) {
                ++inputStart;
            }
        }
        while (outputStart < encoded.length && encoded[outputStart] == ENCODED_ZERO) {
            ++outputStart;
        }
        // Handle 0
        while (--zeros >= 0) {
            encoded[--outputStart] = ENCODED_ZERO;
        }
        // return Base58
        return new String(encoded, outputStart, encoded.length - outputStart);
    }
    public static byte[] decode(String input) {
        if (input.length() == 0) {
            return new byte[0];
        }
        //  BASE58 coding ASCII Character conversion BASE58 Character sequence
        byte[] input58 = new byte[input.length()];
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            int digit = c < 128 ? INDEXES[c] : -1;
            if (digit < 0) {
                String msg = "Invalid characters,c=" + c;
                throw new RuntimeException(msg);
            }
            input58[i] = (byte) digit;
        }
        int zeros = 0;
        while (zeros < input58.length && input58[zeros] == 0) {
            ++zeros;
        }
        // Base58 coding conversion
        byte[] decoded = new byte[input.length()];
        int outputStart = decoded.length;
        for (int inputStart = zeros; inputStart < input58.length; ) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256);
            if (input58[inputStart] == 0) {
                ++inputStart;
            }
        }
        while (outputStart < decoded.length && decoded[outputStart] == 0) {
            ++outputStart;
        }
        // Returns the original byte data
        return Arrays.copyOfRange(decoded, outputStart - zeros, decoded.length);
    }
    // Binary conversion code
    private static byte divmod(byte[] number, int firstDigit, int base, int divisor) {
        int remainder = 0;
        for (int i = firstDigit; i < number.length; i++) {
            int digit = (int) number[i] & 0xFF;
            int temp = remainder * base + digit;
            number[i] = (byte) (temp / divisor);
            remainder = temp % divisor;
        }
        return (byte) remainder;
    }
}