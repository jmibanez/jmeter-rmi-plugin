package com.orangeandbronze.tools.jmeter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.log4j.Logger;

import com.orangeandbronze.tools.jmeter.impl.SimpleLoggingMethodRecorder;
import java.rmi.server.UnicastRemoteObject;

/**
 * <p>A proxy for RMI remote objects.</p>
 *
 *
 * <p>Created: Fri Nov 14 02:34:54 2008</p>
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class NativeRmiProxy
    implements Runnable {

    private String targetRmiName;
    private String actualObjectName;
    private String proxyObjectName;

    private Object stubInstance;
    private DynamicStubProxyInvocationHandler handler;

    private Registry r;
    private int namingPort;
    private int serverPort;

    private ServerSocket eventSocket;
    private volatile boolean stillRunning;
    private static Logger log = Logger.getLogger(NativeRmiProxy.class);

    /**
     * Creates a new <code>NativeRmiProxy</code> instance.
     *
     */
    public NativeRmiProxy(String targetRmiName) {
        int idxSepObjectName = targetRmiName.lastIndexOf("/");
        this.targetRmiName = targetRmiName;
        actualObjectName = targetRmiName.substring(idxSepObjectName + 1);

        proxyObjectName = actualObjectName;
    }

    private void registerProxy(Remote proxy) throws RemoteException, AccessException {
        r.rebind(proxyObjectName, proxy);
    }

    private MethodRecorder createRecorder() throws RemoteException, NotBoundException {
        MethodRecorder impl = new SimpleLoggingMethodRecorder();
        return impl;
    }

    private void setupProxy() {
        try {
            // Create naming registry
            r = LocateRegistry.createRegistry(namingPort);

            // Get stub from actual
            try {
                stubInstance = Naming.lookup(targetRmiName);
            }
            catch(MalformedURLException mfe) {
            }

            // Create recorder
            MethodRecorder r = createRecorder();

            // Build dynamic stub proxy
            Class stub = stubInstance.getClass();
            if(stub == null) {
                throw new RuntimeException("Couldn't find stub class");
            }

            log.debug("Stub class: " + stub.getName());

            Class[] stubInterfaces = stub.getInterfaces();
            Class stubProxyClass = Proxy.getProxyClass(getClass().getClassLoader(),
                                                       stubInterfaces);
            Constructor spCons = stubProxyClass.getConstructor(new Class[] { InvocationHandler.class });
            handler = new DynamicStubProxyInvocationHandler(stubInstance, r);

            Object proxy = spCons.newInstance(new Object[] { handler });

            // Register ourselves on our naming service
            registerProxy(UnicastRemoteObject.exportObject((Remote) proxy, serverPort));
            
        }
        catch(RemoteException remoteEx) {
            log.error(remoteEx);
            throw new RuntimeException(remoteEx);
        }
        catch(NotBoundException boundEx) {
            throw new RuntimeException(boundEx);
        }
        catch(NoSuchMethodException nmEx) {
            log.error(nmEx);
        }
        catch(InvocationTargetException invokEx) {
            throw new RuntimeException(invokEx);
        }
        catch(IllegalAccessException accessEx) {
            throw new RuntimeException(accessEx);
        }
        catch(InstantiationException consEx) {
            throw new RuntimeException(consEx);
        }
    }

    private void readSocketCommand() {
        try {
            Socket sock = eventSocket.accept();
            InputStream sock_inp = sock.getInputStream();
            InputStreamReader sock_rdr = new InputStreamReader(sock_inp, "UTF-8");
            BufferedReader sock_brdr = new BufferedReader(sock_rdr);

            String cmd = sock_brdr.readLine();
            if(cmd != null) {
                sock.close();

                String[] args = cmd.split(" ");
                String verb = args[0].toUpperCase().trim();

                if(verb.equals("EXIT")) {
                    if(args.length > 1) {
                        stillRunning = false;
                    } else {
                        stillRunning = false;
                    }
                    log("DAEMON IS STOPPING...");
                }
            } else {
                // XXX: DO SOMETHING HERE
                logError("No command read from daemon socket");
            }
        } catch(IOException ex) {
            // FIXME: Needs implementation, possibly retry the accept() call?
            logError("IOException: " + ex.getMessage());
        }
    }

    private void logError(String msg) {
        log.error(msg);
    }

    private void log(String msg) {
        log.info(msg);
    }

    private InetAddress getLocalHost()
        throws UnknownHostException {
        return InetAddress.getLocalHost();
    }


    public void run() {
        log("Setting up proxy");
        setupProxy();
        try {
            eventSocket = new ServerSocket(32001, 0, getLocalHost());
        }
        catch(IOException ioEx) {
            return;
        }

        try {
            stillRunning = true;
            while(stillRunning) {
                readSocketCommand();
            }

            log("Packing up...");
        }
        finally {
            if (eventSocket != null) {
                try {
                    eventSocket.close();
                } catch (Exception e) {}
            }
            log("Socket closed");
        }
    }

    public static void main(String[] args) {
        //String targetRmiName, String stubClass
        new Thread(new NativeRmiProxy("//10.10.1.123:1200/server")).start();
    }
}
