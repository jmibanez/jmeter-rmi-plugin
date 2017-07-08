package com.jmibanez.tools.jmeter.util;

import java.rmi.Remote;
import java.util.Base64;
import java.util.UUID;

import com.jmibanez.tools.jmeter.MethodCallRecord;

public class UniqueNameFactory {

    public static final String buildInstanceName(Remote instance) {
        int stubHashCode = instance.hashCode();
        return "remote_I" + stubHashCode;
    }

    public static String encodeLongLongAsBase64(final long msb, final long lsb) {
        byte[] b = new byte[16];
        long v = msb;
        for (int i = 0; i < 8; i++) {
            b[i] = (byte) (v & 0xff);
            v = v >>> 8;
        }
        v = lsb;
        for (int i = 0; i < 8; i++) {
            b[i + 8] = (byte) (v & 0xff);
            v = v >>> 8;
        }

        String rawBase64 = Base64.getUrlEncoder().encodeToString(b);
        return rawBase64.substring(0, rawBase64.length() - 2);
    }

    public static String generateKeyForMethod(final MethodCallRecord r) {
        // Use UUIDv4 as a way to generate unique keys, encoded as base 64
        UUID id = UUID.randomUUID();
        long msb = id.getMostSignificantBits();
        long lsb = id.getLeastSignificantBits();

        return encodeLongLongAsBase64(msb, lsb);
    }
}
