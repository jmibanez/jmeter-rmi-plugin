package com.orangeandbronze.tools.jmeter;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import junit.framework.TestCase;

public class ProxyObjectGraphTest extends TestCase {

    private TestRegistry registry;
    private CallTestRecorder methodRecorder;
    private ProxyObjectGraph graphVisitor;

    public ProxyObjectGraphTest() {
    }

    public void setUp()
        throws Exception {
        registry = new TestRegistry();
        methodRecorder = new CallTestRecorder();
        graphVisitor = new ProxyObjectGraph(registry, methodRecorder);
    }

    public void testVisitSimpleObject()
        throws Exception {

        SimpleBeanInstance simple = new SimpleBeanInstance();
        simple.setName("Simple\nString with \"quotes\" and a \0 null");
        simple.setAge(42);
        simple.c = '\n';

        MethodCallRecord testRecord = new MethodCallRecord();

        SimpleBeanInstance other =
            (SimpleBeanInstance) graphVisitor.replaceRemotes(simple, testRecord);

        // No change
        assertEquals(other, simple);
        assertSame(other, simple);
    }

    public void testVisitSimpleGraph()
        throws Exception {

        SimpleBeanInstance simple = new SimpleBeanInstance();
        simple.setName("Simple\nString with \"quotes\" and a \0 null");
        simple.setAge(42);
        simple.c = '\n';

        List<Object> l = new ArrayList<Object>();
        l.add(simple);

        SimpleBeanInstance simple2 = new SimpleBeanInstance();
        simple2.setName("Simple\nString with \"quotes\" and a \0 null");
        simple2.setAge(42);
        simple2.c = '\n';

        ComplexBeanInstance complex = new ComplexBeanInstance();
        complex.setPersonList(l);
        complex.setOther(simple2);

        MethodCallRecord testRecord = new MethodCallRecord();
        ComplexBeanInstance other =
            (ComplexBeanInstance) graphVisitor.replaceRemotes(complex, testRecord);

        assertEquals(other, complex);
        assertSame(other, complex);
    }

    public void testVisitSimpleGraphWithRemote()
        throws Exception {

        SimpleBeanInstance simple = new SimpleBeanInstance();
        simple.setName("Simple\nString with \"quotes\" and a \0 null");
        simple.setAge(42);
        simple.c = '\n';

        List<Object> l = new ArrayList<Object>();
        l.add(simple);

        SimpleBeanInstance simple2 = new SimpleBeanInstance();
        simple2.setName("Simple\nString with \"quotes\" and a \0 null");
        simple2.setAge(42);
        simple2.c = '\n';

        TestRemote testRemote = new TestRemoteInstance();
        ComplexBeanInstance complex = new ComplexBeanInstance(testRemote);
        complex.setPersonList(l);
        complex.setOther(simple2);

        assertEquals("Local instance returns correctly", 43, testRemote.foo(1));
        assertFalse("Call shouldn't go through proxy", methodRecorder.isRemoteCalled());

        assertEquals("Original object returns correctly", 43, complex.getOtherCall().foo(1));
        assertFalse("Call shouldn't go through proxy", methodRecorder.isRemoteCalled());

        MethodCallRecord testRecord = new MethodCallRecord();
        ComplexBeanInstance other =
            (ComplexBeanInstance) graphVisitor.replaceRemotes(complex, testRecord);
        assertEquals(other, complex);
        assertSame(other, complex);
        assertNotNull("otherCall remote must exist", complex.getOtherCall());

        assertEquals(43, other.getOtherCall().foo(1));
        assertTrue("Call must go through proxy", methodRecorder.isRemoteCalled());
    }


    public void testVisitCyclicGraphWithRemote()
        throws Exception {

        TestRemote testRemote = new TestRemoteInstance();
        CyclicClass parent = new CyclicClass(testRemote);
        CyclicClassChild child = new CyclicClassChild(testRemote);
        child.parent = parent;
        child.name = "child";

        parent.children.add(child);

        assertEquals("Local instance returns correctly", 43, testRemote.foo(1));
        assertFalse("Call shouldn't go through proxy", methodRecorder.isRemoteCalled());

        assertEquals("Original parent returns correctly", 43, parent.getOtherCall().foo(1));
        assertFalse("Call shouldn't go through proxy", methodRecorder.isRemoteCalled());

        assertEquals("Original child returns correctly", 43, child.getOtherCall().foo(1));
        assertFalse("Call shouldn't go through proxy", methodRecorder.isRemoteCalled());

        MethodCallRecord testRecord = new MethodCallRecord();
        CyclicClass otherParent =
            (CyclicClass) graphVisitor.replaceRemotes(parent, testRecord);
        assertEquals(otherParent, parent);
        assertSame(otherParent, parent);

        assertEquals(43, otherParent.getOtherCall().foo(1));
        assertTrue("Call must go through proxy", methodRecorder.isRemoteCalled());

        methodRecorder.resetRemoteCalled();
        CyclicClassChild otherChild = otherParent.children.get(0);
        assertEquals(43, otherChild.getOtherCall().foo(1));
        assertTrue("Call must go through proxy", methodRecorder.isRemoteCalled());
    }


    static class CallTestRecorder
        implements MethodRecorder {

        boolean remoteCalled = false;

        public void recordCall(MethodCallRecord r)
            throws RemoteException {
            remoteCalled = true;
        }

        boolean isRemoteCalled() {
            return remoteCalled;
        }

        void resetRemoteCalled() {
            remoteCalled = false;
        }
    }

    static class TestRegistry
        implements InstanceRegistry {

        private Map<String, Remote> instanceRegistry = new HashMap<>();

        public void registerRootRmiInstance(Remote instance)
            throws RemoteException {
            instanceRegistry.put(null, instance);
        }
        public String registerRmiInstance(String handle, Remote instance)
            throws RemoteException {
            instanceRegistry.put(handle, instance);
            return handle;
        }
        public Remote getTarget(String handle)
            throws RemoteException {
            return instanceRegistry.get(handle);
        }
    }



    public static class SimpleBeanInstance {
        private String name;
        private int age;

        public char c;

        public SimpleBeanInstance() {
        }


        public String getName() {
            return this.name;
        }
        public void setName(String argName) {
            this.name = argName;
        }

        public int getAge() {
            return this.age;
        }
        public void setAge(int argAge) {
            this.age = argAge;
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof SimpleBeanInstance)) {
                return false;
            }

            SimpleBeanInstance otherBean = (SimpleBeanInstance) other;

            return name.equals(otherBean.name)
                && age == otherBean.age
                && c == otherBean.c;
        }
    }

    public interface TestRemote {
        public int foo(int i)
            throws RemoteException;
    }

    public static class TestRemoteInstance
        extends UnicastRemoteObject
        implements TestRemote {

        public TestRemoteInstance()
            throws RemoteException {
            super();
        }

        public int foo(int i)
            throws RemoteException {
            return 42 + i;
        }
    }

    public static class ComplexBeanInstance {
        private List personList;
        private Map someMap;

        private SimpleBeanInstance other;
        private TestRemote otherCall;

        public ComplexBeanInstance() {
        }

        public ComplexBeanInstance(TestRemote otherCall) {
            this.otherCall = otherCall;
        }

        public final List getPersonList() {
            return this.personList;
        }
        public final void setPersonList(final List argPersonList) {
            this.personList = argPersonList;
        }

        public final SimpleBeanInstance getOther() {
            return this.other;
        }
        public final void setOther(final SimpleBeanInstance argOther) {
            this.other = argOther;
        }

        public final Map getSomeMap() {
            return this.someMap;
        }
        public final void setSomeMap(final Map argSomeMap) {
            this.someMap = argSomeMap;
        }

        public final TestRemote getOtherCall() {
            return this.otherCall;
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof ComplexBeanInstance)) {
                return false;
            }

            ComplexBeanInstance otherBean = (ComplexBeanInstance) other;

            return this.other.equals(otherBean.other)
                && personList.equals(otherBean.personList);
        }

    }

    public static class CyclicClass {
        private TestRemote otherCall;
        public List<CyclicClassChild> children = new ArrayList<CyclicClassChild>();

        CyclicClass() {
        }

        CyclicClass(TestRemote otherCall) {
            this.otherCall = otherCall;
        }

        public final TestRemote getOtherCall() {
            return this.otherCall;
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof CyclicClass)) {
                return false;
            }

            CyclicClass otherCyclicClass = (CyclicClass) other;
            return children.equals(otherCyclicClass.children);
        }
    }

    public static class CyclicClassChild {
        private TestRemote otherCall;

        public CyclicClass parent;
        public String name;

        CyclicClassChild() {
        }

        CyclicClassChild(TestRemote otherCall) {
            this.otherCall = otherCall;
        }

        public final TestRemote getOtherCall() {
            return this.otherCall;
        }

        public boolean equals(Object other) {
            if(!(other instanceof CyclicClassChild)) {
                return false;
            }

            CyclicClassChild otherCyclicClassChild = (CyclicClassChild) other;
            return (name == null && otherCyclicClassChild.name == null) || name.equals(otherCyclicClassChild.name);
        }
    }
}
