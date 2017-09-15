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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class SimpleLoggingMethodRecorder
    extends UnicastRemoteObject
    implements MethodRecorder, Serializable
{
    private static final long serialVersionUID = 2124L;
    private static Log log = LogFactory.getLog(SimpleLoggingMethodRecorder.class);

    public SimpleLoggingMethodRecorder()
        throws RemoteException {
        super();
    }

    public void recordCall(MethodCallRecord r)
        throws RemoteException {
        log.debug("Called method: " + r.getMethod());
    }
}
