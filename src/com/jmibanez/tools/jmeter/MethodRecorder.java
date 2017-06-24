/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.jmibanez.tools.jmeter;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Describe interface MethodRecorder here.
 *
 *
 * Created: Fri Nov 14 10:56:53 2008
 *
 * @author <a href="mailto:jm@jmibanez.com">JM Ibanez</a>
 * @version 1.0
 */
public interface MethodRecorder 
    extends Remote
{
    public void recordCall(MethodCallRecord r)
        throws RemoteException;
}
