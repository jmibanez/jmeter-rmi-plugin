/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.orangeandbronze.tools.jmeter.util;

import junit.framework.TestCase;
import bsh.Interpreter;

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
        inst = ScriptletGenerator.getInstance();
        bshInterpreter = new Interpreter();
    }


    public void testSimpleGenerateScriptletForObject() 
        throws Exception {

        SimpleBeanInstance simple = new SimpleBeanInstance();
        simple.setName("Simple\nString with \"quotes\" and a \0 null");
        simple.setAge(42);
        simple.c = '\n';

        String scriptlet = inst.generateScriptletForObject(simple, "simple");

        // TODO: Get a beanshell instance, run scriptlet, assert simple.equals(scriptletInstance)
        assertNotNull(scriptlet);

        bshInterpreter.eval(scriptlet);

        SimpleBeanInstance fromScriptlet = (SimpleBeanInstance) bshInterpreter.get("simple");
        assertEquals(simple, fromScriptlet);

    }

    public void testNestedGenerateScriptletForObject()
        throws Exception {
        assertTrue(true);
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
}
