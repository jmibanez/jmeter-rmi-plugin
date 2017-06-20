package com.orangeandbronze.tools.jmeter;

import java.rmi.Remote;
import junit.framework.TestCase;

public class RMIRemoteObjectConfigTest extends TestCase {

    private RMIRemoteObjectConfig.RemoteRegistry inst;

    @Override
    public void setUp()
        throws Exception {
        inst = new RMIRemoteObjectConfig.RemoteRegistry();
    }


    public void testRegisterNullInstance()
        throws Exception {

        StubDummy d = new StubDummy();
        inst.registerRootRmiInstance(d);

        Remote r = inst.getTarget(null);
        assertNotNull(r);
        assertEquals(d, r);
    }


    private static interface StubDummyInterface
        extends Remote {
        public void foo();
    }

    private static final class StubDummy
        implements Remote {
        public final void foo() {
        }
    }
}
