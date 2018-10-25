package io.baic.cli.internal.common;

import java.util.Arrays;

public class Base64 {
    static String charmap = ".12345abcdefghijklmnopqrstuvwxyz";

    public static String encode(long data) {
    byte[] str = new byte[13];
        Arrays.fill(str, (byte)'.');

    long tmp = data ;
        for ( int i = 0 ; i <=12 ; i++ ){
        byte c = (byte)charmap.charAt((int)(tmp & (i==0 ? 0x0f : 0x1f))) ;
        str[12-i] = c;
        tmp >>= (i==0? 4 : 5);
    }

    int lastIndex = 0 ;
        for ( int j = str.length - 1 ; j >= 0 ; j--) {
        if ( str[j] == '.') {
            lastIndex = j;
        }
    }

    String result = new String(str, 0, lastIndex);

        return result;
}

    public static long decode(String data) {
        long result = 0 ;
        int length = data.length();

        for ( int i = 0 ; i <= 12 ; i++ ){
            long c = 0 ;
            if ( i < length && i <= 12 ) c = char_to_symbol(data.charAt(i)) ;

            if ( i < 12 ) {
                c &= 0x1f;
                c <<= 64-5*(i+1);
            }
            else {
                c &= 0x0f;
            }

            result |= c;
        }

        return result;
    }

    private static byte char_to_symbol (char c) {
        if ( c== '.' ) {
            return 0;
        }

        if ( c== '-' ) {
            return 1;
        }

        if (c >= 'A' && c <= 'Z') {
            return (byte)((c - 'A') + 2);
        }
        if( c >= 'a' && c <= 'z' )
            return (byte)((c - 'a') + 28);

        if( c >= '2' && c <= '7' )
            return (byte)((c - '2') + 58);

        return 0;
    }
}
