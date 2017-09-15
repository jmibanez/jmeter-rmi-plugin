package com.jmibanez.tools.jmeter;

import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

public class MethodCallRecordTest extends TestCase {

    public void testConstructSimpleMethodName() {
        String[] builtNames = MethodCallRecord.constructMethodName("foo", null);
        assertEquals("foo:", builtNames[0]);
        assertEquals("", builtNames[1]);
    }

    public void testConstructMethodNameWithOneArg() {
        String[] builtNames = MethodCallRecord
            .constructMethodName("foo", new Class<?>[] { String.class });
        assertEquals("foo:java.lang.String", builtNames[0]);
        assertEquals("java.lang.String", builtNames[1]);
    }

    public void testConstructMethodNameWithSeveralArgs() {
        String[] builtNames = MethodCallRecord
            .constructMethodName("foo", new Class<?>[] { String.class, Class.class });
        assertEquals("foo:java.lang.String,java.lang.Class", builtNames[0]);
        assertEquals("java.lang.String,java.lang.Class", builtNames[1]);
    }
}
