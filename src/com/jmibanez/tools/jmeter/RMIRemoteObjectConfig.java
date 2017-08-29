/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.jmibanez.tools.jmeter;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.Naming;
import java.util.HashMap;
import java.util.Map;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import com.jmibanez.tools.jmeter.gui.RMIRemoteObjectConfigGUI;
import com.jmibanez.tools.jmeter.impl.RemoteRegistry;
import com.jmibanez.tools.jmeter.impl.SwitchingRemoteRegistry;

/**
 * Describe class RMIRemoteObjectConfig here.
 *
 *
 * Created: Fri Nov 14 13:08:07 2008
 *
 * @author <a href="mailto:jm@jmibanez.com">JM Ibanez</a>
 * @version 1.0
 */
public class RMIRemoteObjectConfig
    extends ConfigTestElement
    implements ThreadListener {

    public static final long serialVersionUID = 43339L;

    public static final String TARGET_RMI_NAME = "RmiRemoteObjectConfig.target_rmi_name";
    public static final String REMOTE_INSTANCES = "RMIRemoteObject.instances";

    private static Log log = LogFactory.getLog(RMIRemoteObjectConfig.class);

    private ThreadLocal<RemoteRegistry> registry = new ThreadLocal<>();
    private ThreadLocal<Objenesis> factory = new ThreadLocal<>();

    /**
     * Creates a new <code>RMIRemoteObjectConfig</code> instance.
     *
     */
    public RMIRemoteObjectConfig() {
    }

    @Override
    public boolean expectsModification() {
        return true;
    }

    public Class<?> getGuiClass() {
        return com.jmibanez.tools.jmeter.gui.RMIRemoteObjectConfigGUI.class;
    }

    public Class<?>[] getArgumentTypes(final String targetName, final String methodName) {
        return getRegistry().getArgumentTypes(targetName, methodName);
    }

    public void setArgumentTypes(String targetName, String methodName, Class<?>[] argTypes) {
        getRegistry().setArgumentTypes(targetName, methodName, argTypes);
    }

    public void threadStarted() {
        log.info("Configuring remote stub registry for thread");

        this.registry.set(new RemoteRegistry());
        this.factory.set(new ObjenesisStd());

        JMeterContext jmctx = JMeterContextService.getContext();
        if(jmctx.getVariables().getObject(REMOTE_INSTANCES) == null) {
            InstanceRegistry switchRegistry = new SwitchingRemoteRegistry();
            jmctx.getVariables().putObject(REMOTE_INSTANCES, switchRegistry);
        }
    }

    public void threadFinished() {
        registry.remove();
        factory.remove();
    }

    public Objenesis getFactory() {
        return this.factory.get();
    }

    public Remote getTarget(final String targetName) {
        assert (targetName != null && getRegistry().getTarget(targetName) != null): "Map should contain key";

        Remote target = getRegistry().getTarget(targetName);
        if(target == null && targetName == null) {
            try {
                target = Naming.lookup(getTargetRmiName());
                getRegistry().registerRootRmiInstance(target);
            }
            catch(Exception ignored) {
                throw new RuntimeException(ignored);
            }
        }
        else {
            // log.warn("target == null");
        }

        return target;
    }

    public String getTargetRmiName() {
        return getPropertyAsString(TARGET_RMI_NAME);
    }

    public void setTargetRmiName(String value) {
        setProperty(new StringProperty(TARGET_RMI_NAME, value));
    }

    public RemoteRegistry getRegistry() {
        return this.registry.get();
    }
}
