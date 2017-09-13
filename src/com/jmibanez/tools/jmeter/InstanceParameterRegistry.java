package com.jmibanez.tools.jmeter;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InstanceParameterRegistry {
    public Class<?>[] getArgumentTypes(String handle, String methodName);
    public void setArgumentTypes(String handle, String methodName,
                                 Class<?>[] argTypes);
}
