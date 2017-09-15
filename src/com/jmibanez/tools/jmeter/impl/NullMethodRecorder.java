/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.jmibanez.tools.jmeter.impl;

import java.rmi.server.UnicastRemoteObject;
import com.jmibanez.tools.jmeter.MethodRecorder;
import com.jmibanez.tools.jmeter.MethodCallRecord;
import java.rmi.RemoteException;
import java.io.Serializable;

/**
 * Describe class SimpleLoggingMethodRecorder here.
 *
 *
 * Created: Fri Nov 14 10:57:37 2008
 *
 * @author <a href="mailto:jm@jmibanez.com">JM Ibanez</a>
 * @version 1.0
 */
public class NullMethodRecorder
    implements MethodRecorder, Serializable
{
    private static final long serialVersionUID = 22345L;

    public NullMethodRecorder() {
    }

    public void recordCall(MethodCallRecord r)
        throws RemoteException {
    }
}
