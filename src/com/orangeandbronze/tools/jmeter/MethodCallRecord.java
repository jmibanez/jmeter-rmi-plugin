package com.orangeandbronze.tools.jmeter;

import java.lang.reflect.Method;
import java.io.Serializable;

public class MethodCallRecord
    implements Serializable
{
    private String method;
    private Object[] args;
    private Object returnValue;

    public MethodCallRecord(Method m, Object[] args, Object returnValue) {
        this.method = m.getName();
        this.args = args;
        this.returnValue = returnValue;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return args;
    }

    public Object getReturnValue() {
        return returnValue;
    }
}

