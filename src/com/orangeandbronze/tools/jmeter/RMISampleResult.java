package com.orangeandbronze.tools.jmeter;

import org.apache.jmeter.samplers.SampleResult;
import java.lang.reflect.Method;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
     * Sets the value of returnValue, as well as constructing sampler response data.
     *
     * @param argReturnValue Value to assign to this.returnValue
     */
    public final void setReturnValue(final Object argReturnValue) {
        this.returnValue = argReturnValue;

        if (returnValue instanceof Throwable) {
            // Response data == exception stack trace
            StringWriter sw = new StringWriter();
            PrintWriter stackTrace = new PrintWriter(sw);

            Throwable t = (Throwable) returnValue;
            t.printStackTrace(stackTrace);

            this.setResponseData(sw.toString());
        }
        else if(returnValue != null) {
            // Serialize the return value
            ByteArrayOutputStream bstream = new ByteArrayOutputStream();

            try {
                ObjectOutputStream objStream = new ObjectOutputStream(bstream);
                objStream.writeObject(returnValue);
            }
            catch(IOException ioEx) {
                PrintWriter stackTrace = new PrintWriter(new OutputStreamWriter(bstream));

                stackTrace.println("ERR: Couldn't serialize return value:");
                ioEx.printStackTrace(stackTrace);
            }
            finally {
                try {
                    bstream.close();
                }
                catch(IOException wtf) { assert false : "IOException closing a simple byte array stream. !?!?"; }
                this.setResponseData(bstream.toByteArray());
            }
        }
        else {
            this.setResponseData("Returned null or method return type is void");
        }
    }

}
