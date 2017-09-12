package com.jmibanez.tools.jmeter;

import java.rmi.Remote;
import java.util.concurrent.CyclicBarrier;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import junit.framework.TestCase;

import com.jmibanez.tools.jmeter.impl.RemoteRegistry;

public class RMIRemoteObjectConfigTest extends TestCase {

    private RemoteRegistry registry;
    private RMIRemoteObjectConfig remoteObjectConfig;

    @Override
    public void setUp()
        throws Exception {
        registry = new RemoteRegistry();
        remoteObjectConfig = new RMIRemoteObjectConfig();
    }


    public void testRegisterNullRegistryance()
        throws Exception {

        StubDummy d = new StubDummy();
        registry.registerRootRmiInstance(d);

        Remote r = registry.getTarget(null);
        assertNotNull(r);
        assertEquals(d, r);
    }


    public void testShouldHaveThreadLocalRegistry()
        throws Exception {

        final StubDummy d1 = new StubDummy();
        final StubDummy d2 = new StubDummy();
        final StubDummy d3 = new StubDummy();

        final CyclicBarrier barrier = new CyclicBarrier(2);
        final Remote[] targets = new Remote[2];

        JMeterContext jmctx = JMeterContextService.getContext();
        jmctx.setVariables(new JMeterVariables());

        Thread t1 = new Thread(new Runnable() {
                public void run() {
                    try {
                        JMeterContext jmctx = JMeterContextService.getContext();
                        jmctx.setVariables(new JMeterVariables());
                        remoteObjectConfig.threadStarted();
                        barrier.await();
                        remoteObjectConfig.getRegistry().registerRootRmiInstance(d1);
                        targets[0] = remoteObjectConfig.getTarget(null);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        Thread t2 = new Thread(new Runnable() {
                public void run() {
                    try {
                        JMeterContext jmctx = JMeterContextService.getContext();
                        jmctx.setVariables(new JMeterVariables());
                        remoteObjectConfig.threadStarted();
                        remoteObjectConfig.getRegistry().registerRootRmiInstance(d2);
                        barrier.await();
                        targets[1] = remoteObjectConfig.getTarget(null);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        remoteObjectConfig.threadStarted();
        remoteObjectConfig.getRegistry().registerRootRmiInstance(d3);
        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertNotNull(remoteObjectConfig.getTarget(null));
        assertNotNull(targets[0]);
        assertNotNull(targets[1]);

        assertEquals(d3, remoteObjectConfig.getTarget(null));
        assertEquals(d1, targets[0]);
        assertEquals(d2, targets[1]);

        assertSame(d3, remoteObjectConfig.getTarget(null));
        assertSame(d1, targets[0]);
        assertSame(d2, targets[1]);

        assertNotSame(d3, targets[0]);
        assertNotSame(d3, targets[1]);
        assertNotSame(targets[0], targets[1]);
    }

    public void testShouldHaveGlobalRegistry()
        throws Exception {

        remoteObjectConfig.setGlobal(true);
        remoteObjectConfig.testStarted();

        final StubDummy d1 = new StubDummy();
        final StubDummy d2 = new StubDummy();
        final StubDummy d3 = new StubDummy();

        final CyclicBarrier barrier = new CyclicBarrier(2);
        final Remote[] targets = new Remote[2];

        JMeterContext jmctx = JMeterContextService.getContext();
        jmctx.setVariables(new JMeterVariables());

        Thread t1 = new Thread(new Runnable() {
                public void run() {
                    try {
                        JMeterContext jmctx = JMeterContextService.getContext();
                        jmctx.setVariables(new JMeterVariables());
                        remoteObjectConfig.threadStarted();
                        barrier.await();
                        remoteObjectConfig.getRegistry().registerRootRmiInstance(d1);
                        targets[0] = remoteObjectConfig.getTarget(null);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        Thread t2 = new Thread(new Runnable() {
                public void run() {
                    try {
                        JMeterContext jmctx = JMeterContextService.getContext();
                        jmctx.setVariables(new JMeterVariables());
                        remoteObjectConfig.threadStarted();
                        remoteObjectConfig.getRegistry().registerRootRmiInstance(d2);
                        barrier.await();
                        targets[1] = remoteObjectConfig.getTarget(null);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        remoteObjectConfig.threadStarted();
        remoteObjectConfig.getRegistry().registerRootRmiInstance(d3);
        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertNotNull(remoteObjectConfig.getTarget(null));
        assertNotNull(targets[0]);
        assertNotNull(targets[1]);

        assertSame(remoteObjectConfig.getTarget(null), targets[0]);
        assertSame(remoteObjectConfig.getTarget(null), targets[1]);
        assertSame(targets[0], targets[1]);
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
