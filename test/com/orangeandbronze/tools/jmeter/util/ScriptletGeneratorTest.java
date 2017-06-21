/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.orangeandbronze.tools.jmeter.util;

import junit.framework.TestCase;
import bsh.Interpreter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Describe class ScriptletGeneratorTest here.
 *
 *
 * Created: Wed Jan 28 14:43:57 2009
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
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

    public void testNestedGenerateScriptletForObject()
        throws Exception {
        SimpleBeanInstance simple = new SimpleBeanInstance();
        simple.setName("Simple\nString with \"quotes\" and a \0 null");
        simple.setAge(42);
        simple.c = '\n';

        List l = new ArrayList();
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


    public static class SimpleBeanInstance
    {

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


    public static class ComplexBeanInstance
    {
        private List personList;
        private Map someMap;

        private SimpleBeanInstance other;

        public ComplexBeanInstance() {
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
    {
        public List<CyclicClassChild> children = new ArrayList<CyclicClassChild>();

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
    {
        public CyclicClass parent;
        public String name;

        public boolean equals(Object other) {
            if(!(other instanceof CyclicClassChild)) {
                return false;
            }

            CyclicClassChild otherCyclicClassChild = (CyclicClassChild) other;
            return (name == null && otherCyclicClassChild.name == null) || name.equals(otherCyclicClassChild.name);
        }
    }
}
