package com.orangeandbronze.tools.jmeter;

import org.apache.jmeter.samplers.SampleResult;
import java.lang.reflect.Method;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.ObjectInputStream;

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

    private transient String packedMethodName;
    private transient String classOwnerName;

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
        if(method == null && packedMethodName != null) {
            // TODO: Lookup and bind method
        }
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

            this.setResponseData(sw.toString(), "UTF-8");
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
            this.setResponseData("Returned null or method return type is void", "UTF-8");
        }
    }


    private void writeObject(ObjectOutputStream out)
        throws IOException {
        out.writeUTF("RMISampleResult");
        if(method != null) {
            out.writeUTF(method.getClass().getCanonicalName());
            out.writeUTF("\nM:");
            out.writeUTF(MethodCallRecord.constructMethodName(method.getName(), method.getParameterTypes()));
        }
        else {
            out.writeUTF(classOwnerName);
            out.writeUTF("\nM:");
            out.writeUTF(packedMethodName);
        }
        out.writeUTF("\nA:");
        if(arguments != null) {
            out.writeInt(arguments.length);
            for(Object arg : arguments) {
                out.writeObject(arg);
            }
        }
        else {
            out.writeInt(0);
        }
        out.writeUTF("EndSample");
    }

    private void readObject(ObjectInputStream in) 
        throws IOException {
        String head = in.readUTF();
        if(!"RMISampleResult".equals(head)) {
            throw new IllegalStateException("Invalid state in input stream: Object header not found: expected RMISampleResult, got " + head);
        }

        classOwnerName = in.readUTF();
        String sep = in.readUTF();
        packedMethodName = in.readUTF();
        String sep2 = in.readUTF();


        int argCount = in.readInt();

        if(argCount > 0) {
            arguments = new Object[argCount];
            for(int i = 0; i < argCount; i++) {
                try {
                    arguments[i] = in.readObject();
                }
                catch(ClassNotFoundException cnfe) {
                    throw new IllegalStateException("Couldn't deserialize arguments:", cnfe);
                }
                catch(NoClassDefFoundError classDefErr) {
                    throw new IllegalStateException("Couldn't deserialize arguments:", classDefErr);
                }
            }
        }

        String footer = in.readUTF();
    }

}
