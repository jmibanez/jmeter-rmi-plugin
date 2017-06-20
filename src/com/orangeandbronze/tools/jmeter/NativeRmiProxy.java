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

//import org.apache.log4j.Logger;
import org.apache.log.Logger;

import com.orangeandbronze.tools.jmeter.impl.SimpleLoggingMethodRecorder;
import com.orangeandbronze.tools.jmeter.impl.NullMethodRecorder;
import java.rmi.server.UnicastRemoteObject;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.util.BeanShellInterpreter;
import org.apache.jorphan.util.JMeterException;

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

    private String bindingScript;

    private Object stubInstance;
    private DynamicStubProxyInvocationHandler handler;

    private Registry r;
    private int namingPort;
    private int serverPort;

    private ServerSocket eventSocket;
    private volatile boolean stillRunning;
    private static Logger log = LoggingManager.getLoggerForClass(); // Logger.getLogger(NativeRmiProxy.class);

    private MethodRecorder recorder;

    /**
     * Creates a new <code>NativeRmiProxy</code> instance.
     *
     */
    public NativeRmiProxy(String targetRmiName) {
        int idxSepObjectName = targetRmiName.lastIndexOf("/");
        this.targetRmiName = targetRmiName;
        actualObjectName = targetRmiName.substring(idxSepObjectName + 1);

        proxyObjectName = actualObjectName;
        recorder = new NullMethodRecorder();
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getNamingPort() {
        return namingPort;
    }

    public void setNamingPort(int namingPort) {
        this.namingPort = namingPort;
    }

    public String getBindingScript() {
        return bindingScript;
    }

    public void setBindingScript(String bindingScript) {
        this.bindingScript = bindingScript;
    }

    public MethodRecorder getMethodRecorder() {
        return recorder;
    }

    public void setMethodRecorder(MethodRecorder recorder) {
        this.recorder = recorder;
    }


    private void registerProxy(Remote proxy) throws RemoteException, AccessException {
        r.rebind(proxyObjectName, proxy);
    }

    private void unregisterProxy() throws RemoteException, NotBoundException {
        r.unbind(proxyObjectName);
    }

    private static Registry createOrLocateRegistry(int port) throws RemoteException {
        return LocateRegistry.createRegistry(port);
    }

    private void runBindingScript(Object serverStub, Object proxy) {
        BeanShellInterpreter bshInterpreter = null;
        try {
            String initFileName = JMeterUtils.getProperty("rmiProxy.bindScriptInitFile");
            bshInterpreter = new BeanShellInterpreter();
        } catch (ClassNotFoundException e) {
        }

        if(bshInterpreter != null) {
            try {
                bshInterpreter.set("FileName", "proxy-binding-script");
                bshInterpreter.set("proxy", proxy);
                bshInterpreter.set("serverStub", serverStub);
                bshInterpreter.set("handler", handler);

                bshInterpreter.eval(bindingScript);
            }
            catch(JMeterException e) {
                log.warn("Couldn't execute BeanShell scriptlet for binding script: ", e);
                e.printStackTrace();
            }
        }
    }


    private void setupProxy() {
        try {
            // Create naming registry
            r = createOrLocateRegistry(namingPort);

            // Get stub from actual
            try {
                stubInstance = Naming.lookup(targetRmiName);
            }
            catch(MalformedURLException mfe) {
            }

            // Create recorder
            MethodRecorder r = getMethodRecorder();

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
            runBindingScript(stubInstance, proxy);
            
        }
        catch(RemoteException remoteEx) {
            log.error("RemoteException thrown while setting up proxy", remoteEx);
            throw new RuntimeException(remoteEx);
        }
        catch(NotBoundException boundEx) {
            throw new RuntimeException(boundEx);
        }
        catch(NoSuchMethodException nmEx) {
            log.error("No such method found, while setting up proxy", nmEx);
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
            eventSocket = new ServerSocket(32002, 0, getLocalHost());
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

            try {
                unregisterProxy();
            } catch (Exception e) {}
        }
    }

    public void stop() {
        stillRunning = false;
    }

    public static void main(String[] args) {
        //String targetRmiName, String stubClass
        new Thread(new NativeRmiProxy("//10.10.1.123:1200/server")).start();
    }
}
