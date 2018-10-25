package io.baic.cli.internal.wallet;

import io.baic.cli.internal.common.Action;
import io.baic.cli.internal.common.Authority;
import io.baic.cli.internal.common.Base32;
import io.baic.cli.internal.common.PermissionLevel;

import java.nio.ByteBuffer;

public class BufferUtil {
    public static int getUint(ByteBuffer buf) {
        int value = 0;
        int by = 0;
        byte tmp;
        do {
            tmp = buf.get();
            value = (tmp & 0x7F) << by;
            by += 7;
        } while ( (tmp & 0x80) > 0 );
        return value;
    }

    public static byte[] packUInt(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        do {
            byte b = (byte)(value & 0x7F);
            value >>= 7;
            b |= (((value > 0) ? 1 : 0) << 7);
            buffer.put(b);
        } while ( value > 0 );
        buffer.flip();
        return buffer.array();
    }

    public static void packAaction(ByteBuffer buffer, Action action) {
        buffer.putLong(Base32.decode(action.account));
        buffer.putLong(Base32.decode(action.name));

        buffer.put(packUInt(action.authorization.size()));
        for (PermissionLevel auth : action.authorization) {
            buffer.putLong(Base32.decode(auth.actor));
            buffer.putLong(Base32.decode(auth.permission));
        }

    }
    
}
