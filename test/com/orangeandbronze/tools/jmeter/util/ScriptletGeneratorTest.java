/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.orangeandbronze.tools.jmeter.util;

import junit.framework.TestCase;

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
        assertEquals("com.orangeandbronze.tools.jmeter.util.ScriptletGeneratorTest.SimpleBeanInstance simple = new com.orangeandbronze.tools.jmeter.util.ScriptletGeneratorTest.SimpleBeanInstance();\n// ----------  Introspection values\nint simple_age = 42;\nsimple.setAge(simple_age);\nString simple_name = \"Simple\\nString with \\\"quotes\\\" and a \\u0000 null\";\nsimple.setName(simple_name);\n\n// ----------  Public field values\nchar simple_c = '\\n';\nsimple.c = simple_c;\n", scriptlet);
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
    }
}
