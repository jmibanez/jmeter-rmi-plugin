/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.jmibanez.tools.jmeter.util;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import junit.framework.TestCase;
import bsh.Interpreter;


/**
 * Describe class ScriptletGeneratorTest here.
 *
 *
 * Created: Wed Jan 28 14:43:57 2009
 *
 * @author <a href="mailto:jm@jmibanez.com">JM Ibanez</a>
 * @version 1.0
 */
public class ScriptletGeneratorTest extends TestCase {

    private ScriptletGenerator inst;
    private Interpreter bshInterpreter;


    /**
     * Creates a new <code>ScriptletGeneratorTest</code> instance.
     *
     */
    public ScriptletGeneratorTest() {
    }

    @Override
    public void setUp()
        throws Exception {
        inst = new ScriptletGenerator();
        bshInterpreter = new Interpreter();
    }


    public void testSimpleGenerateScriptletForObject()
        throws Exception {

        SimpleBeanInstance simple = new SimpleBeanInstance();
        simple.setName("Simple\nString with \"quotes\" and a \0 null");
        simple.setAge(42);
        simple.c = '\n';

        String scriptlet = inst.generateScriptletForObject(simple, "simple");

        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);

        SimpleBeanInstance fromScriptlet = (SimpleBeanInstance) bshInterpreter.get("simple");
        assertEquals(simple, fromScriptlet);

    }

    public void testGenerateScriptletForClassWithoutDefaultConstructor()
        throws Exception {

        SimpleBeanInstance simple = new SimpleBeanInstance();
        simple.setName("Simple\nString with \"quotes\" and a \0 null");
        simple.setAge(42);
        simple.c = '\n';

        SerializableBeanWithoutDefaultConstructor bean
            = new SerializableBeanWithoutDefaultConstructor(simple);

        String scriptlet = inst.generateScriptletForObject(bean, "noCons");

        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);

        SerializableBeanWithoutDefaultConstructor fromScriptlet
            = (SerializableBeanWithoutDefaultConstructor) bshInterpreter.get("noCons");
        assertNotNull(fromScriptlet);

        SimpleBeanInstance simpleFromScriptlet = fromScriptlet.getOther();
        assertNotNull(simpleFromScriptlet);
        assertEquals(simple, simpleFromScriptlet);

    }

    public void testNestedGenerateScriptletForObject()
        throws Exception {
        SimpleBeanInstance simple = new SimpleBeanInstance();
        simple.setName("Simple\nString with \"quotes\" and a \0 null");
        simple.setAge(42);
        simple.c = '\n';

        List<SimpleBeanInstance> l = new ArrayList<>();
        l.add(simple);

        SimpleBeanInstance simple2 = new SimpleBeanInstance();
        simple2.setName("Simple\nString with \"quotes\" and a \0 null");
        simple2.setAge(42);
        simple2.c = '\n';

        ComplexBeanInstance complex = new ComplexBeanInstance();
        complex.setPersonList(l);
        complex.setOther(simple2);

        String scriptlet = inst.generateScriptletForObject(complex, "complex");

        // TODO: Get a beanshell instance, run scriptlet, assert simple.equals(scriptletInstance)
        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);

        ComplexBeanInstance fromScriptlet = (ComplexBeanInstance) bshInterpreter.get("complex");
        assertEquals(complex, fromScriptlet);
    }

    public void testGenerateScriptletFromCyclicClassRef()
        throws Exception {
        CyclicClass parent = new CyclicClass();
        CyclicClassChild child = new CyclicClassChild();
        child.parent = parent;
        child.name = "child";

        parent.children.add(child);

        String scriptlet = inst.generateScriptletForObject(parent, "parent");

        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);

        CyclicClass fromScriptlet = (CyclicClass) bshInterpreter.get("parent");
        assertEquals(parent, fromScriptlet);
        assertEquals(parent.children, fromScriptlet.children);
    }

    public void testGenerateScriptletForCharArray()
        throws Exception {
        char[] testChars = new char[]{ 'a', 'b', 'c' };

        String scriptlet = inst.generateScriptletForObject(testChars, "chars");

        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);
        Object charsFromBsh = bshInterpreter.get("chars");
        Class<?> charsFromBshClass = charsFromBsh.getClass();

        assertTrue(charsFromBshClass.isArray());
        assertEquals(char.class, charsFromBshClass.getComponentType());

        char[] fromBsh = (char[]) charsFromBsh;
        assertEquals(testChars.length, fromBsh.length);
        for (int i = 0; i < testChars.length; i++) {
            assertEquals(testChars[i], fromBsh[i]);
        }
    }

    public void testGenerateScriptletForIntArray()
        throws Exception {
        int[] foo = new int[] { 1, 2, 3 };

        String scriptlet = inst.generateScriptletForObject(foo, "arr");

        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);
        Object arrFromBsh = bshInterpreter.get("arr");
        Class<?> arrFromBshClass = arrFromBsh.getClass();

        assertTrue(arrFromBshClass.isArray());
        assertEquals(int.class, arrFromBshClass.getComponentType());

        int[] fromBsh = (int[]) arrFromBsh;
        assertEquals(foo.length, fromBsh.length);
        for (int i = 0; i < foo.length; i++) {
            assertEquals(foo[i], fromBsh[i]);
        }
    }

    public void testGenerateScriptletForStringArray()
        throws Exception {
        String[] foo = new String[] { "1", "2", "3" };

        String scriptlet = inst.generateScriptletForObject(foo, "arr");

        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);
        Object arrFromBsh = bshInterpreter.get("arr");
        Class<?> arrFromBshClass = arrFromBsh.getClass();

        assertTrue(arrFromBshClass.isArray());
        assertEquals(String.class, arrFromBshClass.getComponentType());

        String[] fromBsh = (String[]) arrFromBsh;
        assertEquals(foo.length, fromBsh.length);
        for (int i = 0; i < foo.length; i++) {
            assertEquals(foo[i], fromBsh[i]);
        }
    }

    @SuppressWarnings("rawtypes")
    public void testGenerateScriptletForList()
        throws Exception {
        SimpleBeanInstance a = new SimpleBeanInstance();
        a.setName("Simple\nString with \"quotes\" and a \0 null (A)");
        a.setAge(40);
        a.c = '\n';

        SimpleBeanInstance b = new SimpleBeanInstance();
        b.setName("Simple\nString with \"quotes\" and a \0 null (B)");
        b.setAge(41);
        b.c = '\t';

        SimpleBeanInstance c = new SimpleBeanInstance();
        c.setName("Simple\nString with \"quotes\" and a \0 null (C)");
        c.setAge(42);
        c.c = '?';

        List<SimpleBeanInstance> testList = new ArrayList<>();
        testList.add(a);
        testList.add(b);
        testList.add(c);

        String scriptlet = inst.generateScriptletForObject(testList, "list");

        assertNotNull(scriptlet);
        System.out.println(scriptlet);

        bshInterpreter.eval(scriptlet);
        Object listFromBsh = bshInterpreter.get("list");
        Class<?> listFromBshClass = listFromBsh.getClass();

        assertTrue(List.class.isAssignableFrom(listFromBshClass));

        // No way to recover collection element types, unfortunately
        List fromBsh = (List) listFromBsh;
        assertEquals(testList.size(), fromBsh.size());
        for (int i = 0; i < testList.size(); i++) {
            assertEquals(testList.get(i), fromBsh.get(i));
        }
    }

    public void testGenerateScriptletForArrayWithProperType()
        throws Exception {
        SimpleBeanInstance a = new SimpleBeanInstance();
        a.setName("Simple\nString with \"quotes\" and a \0 null (A)");
        a.setAge(40);
        a.c = '\n';

        SimpleBeanInstance b = new SimpleBeanInstance();
        b.setName("Simple\nString with \"quotes\" and a \0 null (B)");
        b.setAge(41);
        b.c = '\t';

        SimpleBeanInstance c = new SimpleBeanInstance();
        c.setName("Simple\nString with \"quotes\" and a \0 null (C)");
        c.setAge(42);
        c.c = '?';

        SimpleBeanInstance[] testArr = new SimpleBeanInstance[] { a, b, c };

        String scriptlet = inst.generateScriptletForObject(testArr, "arr");

        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);
        Object arrayFromBsh = bshInterpreter.get("arr");
        Class<?> arrayFromBshClass = arrayFromBsh.getClass();

        assertTrue(arrayFromBshClass.isArray());
        assertEquals(SimpleBeanInstance.class,
                     arrayFromBshClass.getComponentType());

        SimpleBeanInstance[] fromBsh = (SimpleBeanInstance[]) arrayFromBsh;
        assertEquals(testArr.length, fromBsh.length);
        for (int i = 0; i < testArr.length; i++) {
            assertEquals(testArr[i], fromBsh[i]);
        }
    }

    public void testGeneratePrimitiveArrayScriptletWithNullValues()
        throws Exception {
        Integer[] testArr = new Integer[] { 0, 1, null };

        String scriptlet = inst.generateScriptletForObject(testArr, "arr");

        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);
        Object arrayFromBsh = bshInterpreter.get("arr");
        Class<?> arrayFromBshClass = arrayFromBsh.getClass();

        assertTrue(arrayFromBshClass.isArray());
        assertEquals(Integer.class,
                     arrayFromBshClass.getComponentType());

        Integer[] fromBsh = (Integer[]) arrayFromBsh;
        assertEquals(testArr.length, fromBsh.length);
        for (int i = 0; i < testArr.length; i++) {
            assertEquals(testArr[i], fromBsh[i]);
        }
    }

    public void testGenerateScriptletForArrayWithProperTypeNullValues()
        throws Exception {
        SimpleBeanInstance a = new SimpleBeanInstance();
        a.setName("Simple\nString with \"quotes\" and a \0 null (A)");
        a.setAge(40);
        a.c = '\n';

        SimpleBeanInstance b = new SimpleBeanInstance();
        b.setName("Simple\nString with \"quotes\" and a \0 null (B)");
        b.setAge(41);
        b.c = '\t';

        SimpleBeanInstance[] testArr = new SimpleBeanInstance[] { a, b, null };

        String scriptlet = inst.generateScriptletForObject(testArr, "arr");

        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);
        Object arrayFromBsh = bshInterpreter.get("arr");
        Class<?> arrayFromBshClass = arrayFromBsh.getClass();

        assertTrue(arrayFromBshClass.isArray());
        assertEquals(SimpleBeanInstance.class,
                     arrayFromBshClass.getComponentType());

        SimpleBeanInstance[] fromBsh = (SimpleBeanInstance[]) arrayFromBsh;
        assertEquals(testArr.length, fromBsh.length);
        for (int i = 0; i < testArr.length; i++) {
            assertEquals(testArr[i], fromBsh[i]);
        }
    }

    public void testGetVariableNameForType()
        throws Exception {
        assertEquals("args", inst.getVariableNameForType(null));

        assertEquals("simpleBeanInstance",
                     inst.getVariableNameForType(new SimpleBeanInstance()));
        assertEquals("arrayList",
                     inst.getVariableNameForType(new ArrayList<Object>()));
        assertEquals("objectArray",
                     inst.getVariableNameForType(new Object[0]));
        assertEquals("intArray",
                     inst.getVariableNameForType(new int[0]));
    }

    public void testSkipNonSerializableValues()
        throws Exception {

        SimpleBeanInstance simple = new SimpleBeanInstance();
        simple.setName("Simple\nString with \"quotes\" and a \0 null");
        simple.setAge(42);
        simple.setMemory(100);
        simple.c = '\n';

        simple.setIntern(new NonSerializable());

        String scriptlet = inst.generateScriptletForObject(simple, "simple");

        assertNotNull(scriptlet);
        System.out.println(scriptlet);

        bshInterpreter.eval(scriptlet);

        SimpleBeanInstance fromScriptlet = (SimpleBeanInstance) bshInterpreter.get("simple");
        assertEquals(simple, fromScriptlet);

        // Ensure that intern is null;
        assertNotNull(simple.getIntern());
        assertNull(fromScriptlet.getIntern());

        // Ensure memory wasn't set
        assertEquals(100, simple.getMemory());
        assertEquals(0, fromScriptlet.getMemory());
    }


    public static class SimpleBeanInstance
        implements Serializable {

        public static final long serialVersionUID = 989L;

        private String name;
        private int age;

        private NonSerializable intern;
        private transient int memory;

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

        public int getMemory() {
            return this.memory;
        }

        public void setMemory(int memory) {
            this.memory = memory;
        }

        public NonSerializable getIntern() {
            return this.intern;
        }

        public void setIntern(NonSerializable intern) {
            this.intern = intern;
        }

        @Override
        public int hashCode() {
            return name.hashCode() << 16 | (age << 8) | (c << 4);
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


    public static class SerializableBeanWithoutDefaultConstructor
        implements Serializable {

        public static final long serialVersionUID = 222224L;

        private SimpleBeanInstance other;

        public SerializableBeanWithoutDefaultConstructor(SimpleBeanInstance other) {
            this.other = other;
        }

        public final SimpleBeanInstance getOther() {
            return this.other;
        }
        public final void setOther(final SimpleBeanInstance argOther) {
            this.other = argOther;
        }
    }


    public static class ComplexBeanInstance
        implements Serializable {

        public static final long serialVersionUID = 222223333L;

        private List<SimpleBeanInstance> personList;
        private Map<String, Object> someMap;

        private SimpleBeanInstance other;

        public ComplexBeanInstance() {
        }


        public final List<SimpleBeanInstance> getPersonList() {
            return this.personList;
        }
        public final void setPersonList(final List<SimpleBeanInstance> argPersonList) {
            this.personList = argPersonList;
        }

        public final SimpleBeanInstance getOther() {
            return this.other;
        }
        public final void setOther(final SimpleBeanInstance argOther) {
            this.other = argOther;
        }

        public final Map<String, Object> getSomeMap() {
            return this.someMap;
        }
        public final void setSomeMap(final Map<String, Object> argSomeMap) {
            this.someMap = argSomeMap;
        }

        @Override
        public int hashCode() {
            int personListHash = (personList != null) ? personList.hashCode() : 0;
            int someMapHash = (someMap != null) ? someMap.hashCode() : 0;
            int otherHash = (other != null) ? other.hashCode() : 0;

            return (personListHash << 24) | (someMapHash << 16)
                | (otherHash << 8);
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

    public static class CyclicClass
        implements Serializable {

        public static final long serialVersionUID = 76144249L;

        public List<CyclicClassChild> children = new ArrayList<CyclicClassChild>();

        @Override
        public int hashCode() {
            return children.hashCode();
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

    public static class CyclicClassChild
        implements Serializable {

        public static final long serialVersionUID = 9911113455L;

        public CyclicClass parent;
        public String name;

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof CyclicClassChild)) {
                return false;
            }

            CyclicClassChild otherCyclicClassChild = (CyclicClassChild) other;
            return (name == null && otherCyclicClassChild.name == null) || name.equals(otherCyclicClassChild.name);
        }
    }

    public static class NonSerializable {
        private int key = 69;

        public int getKey() {
            return key;
        }

        public void setKey(final int key) {
            this.key = key;
        }
    }
}
