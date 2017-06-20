/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.orangeandbronze.tools.jmeter.impl;

import java.rmi.server.UnicastRemoteObject;
import com.orangeandbronze.tools.jmeter.MethodRecorder;
import com.orangeandbronze.tools.jmeter.MethodCallRecord;
import java.rmi.RemoteException;
import java.io.Serializable;

/**
 * Describe class SimpleLoggingMethodRecorder here.
 *
 *
 * Created: Fri Nov 14 10:57:37 2008
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class NullMethodRecorder
    implements MethodRecorder, Serializable
{
    public NullMethodRecorder() {
    }

    public void recordCall(MethodCallRecord r)
        throws RemoteException {
    }
}
