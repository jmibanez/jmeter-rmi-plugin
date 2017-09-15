package com.jmibanez.tools.jmeter.impl;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;

import com.jmibanez.tools.jmeter.InstanceRegistry;
import com.jmibanez.tools.jmeter.RMISampler;
import com.jmibanez.tools.jmeter.RMIRemoteObjectConfig;

/**
 * Remote registry bound in thread variables to support existing
 * post processors. This looks up the existing remote registry by
 * looking up the remote object config element attached to the
 * current RMI sampler.
 */
public class SwitchingRemoteRegistry
    implements InstanceRegistry {

    public void registerRootRmiInstance(Remote instance)
        throws RemoteException {
        getCurrentRegistry().registerRootRmiInstance(instance);
    }

    public String registerRmiInstance(String handle, Remote instance)
        throws RemoteException {
        return getCurrentRegistry().registerRmiInstance(handle, instance);
    }

    public Remote getTarget(String handle)
        throws RemoteException {
        return getCurrentRegistry().getTarget(handle);
    }

    private InstanceRegistry getCurrentRegistry() {
        JMeterContext jmctx = JMeterContextService.getContext();
        RMIRemoteObjectConfig remoteObj = (RMIRemoteObjectConfig) jmctx.getCurrentSampler()
            .getProperty(RMISampler.REMOTE_OBJECT_CONFIG)
            .getObjectValue();
        return remoteObj.getRegistry();
    }
}

