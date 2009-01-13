package com.orangeandbronze.tools.jmeter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.TestElementProperty;

import org.apache.log4j.Logger;
import java.rmi.server.RemoteObject;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import java.rmi.Remote;
import com.orangeandbronze.tools.jmeter.gui.RMISamplerGUI;

/**
 * Describe class RMISampler here.
 *
 *
 * Created: Wed Nov 12 13:16:39 2008
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class RMISampler extends AbstractSampler {

    public static final String REMOTE_OBJECT_CONFIG = "RMISampler.remote_object_config";
    public static final String METHOD_NAME = "RMISampler.method_name";
    public static final String ARGUMENTS = "RMISampler.method_arguments";

    private static Logger log = Logger.getLogger(RMISampler.class);


    /**
     * Creates a new <code>RMISampler</code> instance.
     *
     */
    public RMISampler() {
    }


    public SampleResult sample(Entry e) {
        return sample();
    }

    public void addTestElement(TestElement el) {
        if (el instanceof RMIRemoteObjectConfig) {
            setRemoteObjectConfig((RMIRemoteObjectConfig) el);
        } else {
            super.addTestElement(el);
        }
    }

    public void setRemoteObjectConfig(RMIRemoteObjectConfig value) {
        RMIRemoteObjectConfig remoteObj = getRemoteObjectConfig();
        if (remoteObj != null) {
            log.warn("Existing remote object config " + remoteObj.getName() + " superseded by " + value.getName());
        }
        setProperty(new TestElementProperty(REMOTE_OBJECT_CONFIG, value));
    }

    public RMIRemoteObjectConfig getRemoteObjectConfig() {
        return (RMIRemoteObjectConfig) getProperty(REMOTE_OBJECT_CONFIG).getObjectValue();
    }

    public void setMethodName(String value) {
        setProperty(METHOD_NAME, value);
        setName(value);
    }

    public String getMethodName() {
        return getPropertyAsString(METHOD_NAME);
    }

    public Class getGuiClass() {
        return RMISamplerGUI.class;
    }


    public Object[] getArguments() {
        JMeterProperty p = getProperty(ARGUMENTS);
        if(p == null || p instanceof NullProperty) {
            return null;
        }

        ObjectProperty op = (ObjectProperty) getProperty(ARGUMENTS);
        return (Object[]) op.getObjectValue();
    }

    public void setArguments(Object[] arguments) {
        setProperty(new ObjectProperty(ARGUMENTS, arguments));
    }

    protected SampleResult sample() {
        log.info("Sample called");
        RMISampleResult res = new RMISampleResult();

        RMIRemoteObjectConfig remoteObj = getRemoteObjectConfig();

        String methodName = getMethodName();

        log.info("Getting arguments");
        Object[] args = getArguments();

        log.info("Getting target");
        Remote target = remoteObj.getTarget();

        Class[] argTypes = remoteObj.getArgumentTypes(methodName);

        try {
            Class targetClass = target.getClass();
            String actualMethodName = getMethodName(methodName);
            Method m = targetClass.getMethod(actualMethodName, argTypes);

            res.setMethod(m);
            res.setArguments(args);
            res.sampleStart();
            Object retval = m.invoke(target, args);

            res.sampleEnd();
            res.setReturnValue(retval);
            res.setSuccessful(true);
        }
        catch(NoSuchMethodException noMethod) {
            throw new RuntimeException(noMethod);
        }
        catch(IllegalAccessException accessEx) {
            throw new RuntimeException(accessEx);
        }
        catch(InvocationTargetException invokEx) {
            Throwable actualEx = invokEx.getCause();
            // FIXME: Add to result
            res.sampleEnd();
            res.setReturnValue(actualEx);
            res.setSuccessful(false);
        }

        return res;
    }

    private String getMethodName(String methodNameAndArgs) {
        if(methodNameAndArgs.indexOf(":") > -1) {
            return methodNameAndArgs.substring(0, methodNameAndArgs.indexOf(":"));
        }

        return methodNameAndArgs;
    }
}
