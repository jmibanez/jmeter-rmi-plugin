package com.orangeandbronze.tools.jmeter;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InstanceRegistry
    extends Remote{
    public void registerRootRmiInstance(Remote instance)
        throws RemoteException;
    public String registerRmiInstance(String handle, Remote instance)
        throws RemoteException;
    public Remote getTarget(String handle)
        throws RemoteException;
}
