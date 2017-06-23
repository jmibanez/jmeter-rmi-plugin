package com.orangeandbronze.tools.jmeter;

import java.lang.reflect.Method;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Map;

public class MethodCallRecord
    implements Serializable
{

    private static final long serialVersionUID = -30090001L;

    private String target;
    private String method;
    private Class[] argTypes;
    private Object[] args;
    private byte[] argsPacked;
    private Object returnValue;
    private Throwable returnException;
    private boolean isException = false;

    private transient boolean isRemoteReturned = false;
    private transient Map<String, String> remotePathsInReturn = Collections.emptyMap();

    MethodCallRecord() {
    }

    public MethodCallRecord(String target, Method m, Object[] args) {
        this.target = target;
        this.argTypes = m.getParameterTypes();
        this.method = constructMethodName(m.getName(), this.argTypes);
        this.args = args;
        packArgs();
    }

    public String getTarget() {
        return target;
    }

    public String getMethod() {
        return method;
    }

    public Object[] recreateArguments() {
        unpackArgs();
        return args;
    }

    public Object[] getArguments() {
        return args;
    }

    public Class[] getArgumentTypes() {
        return argTypes;
    }

    public void returned(Object returnValue) {
        this.returnValue = returnValue;
    }

    public void thrown(Throwable t) {
        this.returnValue = t;
        this.isException = true;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public Throwable getReturnValueAsThrowable() {
        if(isException) {
            return (Throwable) returnValue;
        }
        else {
            throw new IllegalStateException("Not an exception");
        }
    }

    public boolean isException() {
        return isException;
    }

    public void setRemoteReturned(final boolean isRemoteReturned) {
        this.isRemoteReturned = isRemoteReturned;
    }

    public boolean isRemoteReturned() {
        return isRemoteReturned;
    }

    public void setRemotePathsInReturn(final Map<String, String> remotePathsInReturn) {
        this.remotePathsInReturn = remotePathsInReturn;
    }

    public Map<String, String> getRemotePathsInReturn() {
        return remotePathsInReturn;
    }

    public static String constructMethodName(String methodName, Class[] argTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName);

        if(argTypes == null || argTypes.length == 0) {
            sb.append(":");
            return sb.toString();
        }

        sb.append(":");
        for(Class c : argTypes) {
            sb.append(c.getName());
            sb.append(",");
        }

        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    private void writeObject(ObjectOutputStream out)
        throws IOException {
        // Custom format, to allow packed argument values
        out.writeUTF("CALL");

        out.writeUTF(method);

        out.writeInt(argsPacked.length);
        out.write(argsPacked);

        out.writeUTF("RETURN");

        out.writeBoolean(isException);
        out.writeObject(returnValue);

        out.writeUTF("END");
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // Custom format, to allow packed argument values
        String head = in.readUTF();
        if(!"CALL".equals(head)) {
            throw new IllegalStateException("Invalid state in input stream: Object header not found");
        }

        method = in.readUTF();

        int packLen = in.readInt();
        argsPacked = new byte[packLen];
        in.read(argsPacked, 0, packLen);

        String ret = in.readUTF();
        if(!"RETURN".equals(ret)) {
            throw new IllegalStateException("Invalid state in input stream: Return value header not found");
        }

        isException = in.readBoolean();
        returnValue = in.readObject();

        String eof = in.readUTF();
        if(!"END".equals(eof)) {
            throw new IllegalStateException("Invalid state in input stream: End of stream not found");
        }

        unpackArgs();
    }

    private void packArgs() {
        try {
            ByteArrayOutputStream packOut = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(packOut);
            ostream.writeObject(args);
            argsPacked = packOut.toByteArray();
        }
        catch(IOException ign) {
            throw new RuntimeException(ign);
        }
    }

    private void unpackArgs() {
        try {
            ByteArrayInputStream packIn = new ByteArrayInputStream(argsPacked);
            ObjectInputStream istream = new ObjectInputStream(packIn);
            args = (Object[]) istream.readObject();
        }
        catch(IOException ign) {
            throw new RuntimeException(ign);
        }
        catch(ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }
}

