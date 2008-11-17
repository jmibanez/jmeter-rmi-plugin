package com.orangeandbronze.tools.jmeter;

import org.apache.jmeter.samplers.SampleResult;
import java.lang.reflect.Method;

/**
 * Describe class RMISampleResult here.
 *
 *
 * Created: Wed Nov 12 13:20:27 2008
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class RMISampleResult
    extends SampleResult {

    private Method method;
    private Object[] arguments;

    private Object returnValue;


    /**
     * Creates a new <code>RMISampleResult</code> instance.
     *
     */
    public RMISampleResult() {
        super();
    }


    /**
     * Gets the value of method
     *
     * @return the value of method
     */
    public final Method getMethod() {
        return this.method;
    }

    /**
     * Sets the value of method
     *
     * @param argMethod Value to assign to this.method
     */
    public final void setMethod(final Method argMethod) {
        this.method = argMethod;
    }

    /**
     * Gets the value of arguments
     *
     * @return the value of arguments
     */
    public final Object[] getArguments() {
        return this.arguments;
    }

    /**
     * Sets the value of arguments
     *
     * @param argArguments Value to assign to this.arguments
     */
    public final void setArguments(final Object[] argArguments) {
        this.arguments = argArguments;
    }

    /**
     * Gets the value of returnValue
     *
     * @return the value of returnValue
     */
    public final Object getReturnValue() {
        return this.returnValue;
    }

    /**
     * Sets the value of returnValue
     *
     * @param argReturnValue Value to assign to this.returnValue
     */
    public final void setReturnValue(final Object argReturnValue) {
        this.returnValue = argReturnValue;
    }

}
