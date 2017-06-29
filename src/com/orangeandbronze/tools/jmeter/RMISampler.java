package com.orangeandbronze.tools.jmeter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.TestElementProperty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.server.RemoteObject;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import java.rmi.Remote;
import com.orangeandbronze.tools.jmeter.gui.RMISamplerGUI;
import bsh.Interpreter;
import bsh.EvalError;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;

/**
 * Describe class RMISampler here.
 *
 *
 * Created: Wed Nov 12 13:16:39 2008
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class RMISampler
    extends AbstractSampler
    implements ThreadListener {

    public static final String REMOTE_OBJECT_CONFIG = "RMISampler.remote_object_config";
    public static final String TARGET_NAME = "RMISampler.target_name";
    public static final String METHOD_NAME = "RMISampler.method_name";
    public static final String ARGUMENTS = "RMISampler.method_arguments";
    public static final String ARG_SCRIPT = "RMISampler.method_arguments_script";

    public static final String IGNORE_EXCEPTIONS = "RMISampler.ignore_exceptions";

    private static Log log = LogFactory.getLog(RMISampler.class);


    private ThreadLocal<Interpreter> interpreter = new ThreadLocal<Interpreter>();

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

    public void threadStarted() {
        Interpreter argInterpreter = new Interpreter();
        interpreter.set(argInterpreter);
        try {
            argInterpreter.eval(getArgumentsScript());
        }
        catch(EvalError evalErr) {
            log.warn("Error initially evaluating script: " + evalErr.getMessage());
        }
    }

    public void threadFinished() {
        interpreter.remove();
    }

    public void setTargetName(final String value) {
        if ("".trim().equals(value)) {
            setProperty(TARGET_NAME, null);

        }
        setProperty(TARGET_NAME, value);
    }

    public String getTargetName() {
        String value = getPropertyAsString(TARGET_NAME);
        if ("".trim().equals(value)) {
            return null;
        }
        return value;
    }

    public void setMethodName(String value) {
        setProperty(METHOD_NAME, value);
        setName(value);
    }

    public String getMethodName() {
        return getPropertyAsString(METHOD_NAME);
    }

    public void setArgumentsScript(String value) {
        setProperty(ARG_SCRIPT, value);
    }

    public String getArgumentsScript() {
        return getPropertyAsString(ARG_SCRIPT);
    }

    public void setExceptionsIgnored(boolean ign) {
        setProperty(IGNORE_EXCEPTIONS, ign);
    }

    public boolean isExceptionsIgnored() {
        return getPropertyAsBoolean(IGNORE_EXCEPTIONS);
    }


    public Class getGuiClass() {
        return RMISamplerGUI.class;
    }


    public Object[] getArguments() {
        return fromArgumentsScript();
    }

    public void setArguments(Object[] arguments) {
        setProperty(new ObjectProperty(ARGUMENTS, arguments));
    }

    private Object[] fromArgumentsScript() {
        JMeterContext ctx = JMeterContextService.getContext();
        JMeterVariables vars = ctx.getVariables();
        Interpreter argInterpreter = getInterpreter();
        try {
            argInterpreter.set("ctx", ctx);
            argInterpreter.set("vars", vars);
            argInterpreter.set("sampler", this);
            return (Object[]) argInterpreter.eval("methodArgs();");
        }
        catch(EvalError evalErr) {
            log.error(getMethodName() + ": Error evaluating script: " + evalErr.getMessage() + "; argInterpreter = " + argInterpreter,
                      evalErr);
        }

        return null;
    }

    protected SampleResult sample() {
        log.debug("Sample called");
        RMISampleResult res = new RMISampleResult();
        res.sampleStart();

        RMIRemoteObjectConfig remoteObj = getRemoteObjectConfig();

        String methodName = getMethodName();

        log.debug("Getting arguments");
        Object[] args = getArguments();
        res.connectEnd();

        log.debug("Getting target");
        String targetName = getTargetName();
        Remote target = remoteObj.getTarget(targetName);

        Class[] argTypes = remoteObj.getArgumentTypes(targetName,
                                                      methodName);

        try {
            Class<?> targetClass = target.getClass();
            String actualMethodName = getMethodName(methodName);
            Method m = targetClass.getMethod(actualMethodName, argTypes);

            res.setMethod(m);
            res.setSampleLabel(methodName);
            res.setArguments(args);

            // Assume success
            res.setSuccessful(true);
            res.latencyEnd();
            Object retval = m.invoke(target, args);

            res.sampleEnd();
            res.setReturnValue(retval);
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

            if(!isExceptionsIgnored()) {
                res.setSuccessful(false);
            }
        }

        return res;
    }

    private String getMethodName(String methodNameAndArgs) {
        if(methodNameAndArgs.indexOf(":") > -1) {
            return methodNameAndArgs.substring(0, methodNameAndArgs.indexOf(":"));
        }

        return methodNameAndArgs;
    }

    private RMIRemoteObjectConfig getRemoteObjectConfig() {
        return (RMIRemoteObjectConfig) getProperty(REMOTE_OBJECT_CONFIG).getObjectValue();
    }

    private void setRemoteObjectConfig(RMIRemoteObjectConfig value) {
        RMIRemoteObjectConfig remoteObj = getRemoteObjectConfig();
        if (remoteObj != null) {
            log.warn("Existing remote object config " + remoteObj.getName() + " superseded by " + value.getName());
        }
        JMeterProperty remoteObjProp = new TestElementProperty(REMOTE_OBJECT_CONFIG, value);
        setProperty(remoteObjProp);
        setTemporary(remoteObjProp);
    }

    private Interpreter getInterpreter() {
        return interpreter.get();
    }

    public String toString() {
        return super.toString() +  ": " +  getMethodName();
    }
}
