package com.orangeandbronze.tools.jmeter.util;

import junit.framework.TestCase;

public class UniqueNameFactoryTest extends TestCase {

    public void testEncodeLongLongAsBase64()
        throws Exception {
        long msb = 0x12345678L;
        long lsb = 0x9ABCDEF0L;

        String encoded = UniqueNameFactory.encodeLongLongAsBase64(msb, lsb);
        assertEquals("eFY0EgAAAADw3ryaAAAAAA", encoded);
    }
}
