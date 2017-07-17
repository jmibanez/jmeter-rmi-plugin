/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.jmibanez.tools.jmeter;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.RecordingController;
import org.apache.jmeter.testelement.WorkBench;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.GuiPackage;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.testelement.property.IntegerProperty;
import com.jmibanez.tools.jmeter.gui.NativeRmiProxyControllerGui;
import com.jmibanez.tools.jmeter.impl.RmiSamplerGeneratorMethodRecorder;
import org.apache.jmeter.extractor.BeanShellPostProcessor;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.modifiers.BeanShellPreProcessor;
import org.apache.jmeter.modifiers.BeanShellPreProcessorBeanInfo;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Array;
import java.util.Collection;
import com.jmibanez.tools.jmeter.util.ScriptletGenerator;

/**
 * Describe class NativeRmiProxyController here.
 *
 *
 * Created: Fri Nov 14 14:32:04 2008
 *
 * @author <a href="mailto:jm@jmibanez.com">JM Ibanez</a>
 * @version 1.0
 */
public class NativeRmiProxyController extends GenericController {

    public static final String TARGET_RMI_NAME = "RmiProxy.target_rmi_name";
    public static final String PROXY_NAMING_PORT = "RmiProxy.proxy_naming_port";
    public static final String PROXY_PORT = "RmiProxy.proxy_port";

    public static final String BINDING_SCRIPT = "RmiProxy.binding_script";

    private static Log log = LogFactory.getLog(NativeRmiProxyController.class);

    private JMeterTreeNode target;

    private NativeRmiProxy proxy;

    /**
     * Creates a new <code>NativeRmiProxyController</code> instance.
     *
     */
    public NativeRmiProxyController() {
    }


    public synchronized void deliverSampler(RMISampler s, BeanShellPostProcessor p,
                                            MethodCallRecord record) {
        JMeterTreeNode myTarget = findTargetControllerNode();
        TestElement[] children = null;
        if (p != null) {
            children = new TestElement[]{ p };
        }
        placeSampler(s, children, myTarget, record);
    }


    public String getTargetRmiName() {
        return getPropertyAsString(TARGET_RMI_NAME);
    }

    public void setTargetRmiName(String value) {
        setProperty(new StringProperty(TARGET_RMI_NAME, value));
    }

    public int getProxyNamingPort() {
        return getPropertyAsInt(PROXY_NAMING_PORT);
    }

    public void setProxyNamingPort(int value) {
        setProperty(new IntegerProperty(PROXY_NAMING_PORT, value));
    }

    public void setProxyNamingPort(String value) {
        setProperty(PROXY_NAMING_PORT, value);
    }

    public int getProxyPort() {
        return getPropertyAsInt(PROXY_PORT);
    }

    public void setProxyPort(int value) {
        setProperty(new IntegerProperty(PROXY_PORT, value));
    }

    public void setProxyPort(String value) {
        setProperty(PROXY_PORT, value);
    }

    public String getBindingScript() {
        return getPropertyAsString(BINDING_SCRIPT);
    }

    public void setBindingScript(String script) {
        setProperty(new StringProperty(BINDING_SCRIPT, script));
    }


    public JMeterTreeNode getTarget() {
        return target;
    }

    public void setTarget(JMeterTreeNode target) {
        this.target = target;
    }

    public Class getGuiClass() {
        return com.jmibanez.tools.jmeter.gui.NativeRmiProxyControllerGui.class;
    }

    private JMeterTreeNode findFirstNodeOfType(Class type) {
        JMeterTreeModel treeModel = GuiPackage.getInstance().getTreeModel();
        List nodes = treeModel.getNodesOfType(type);
        Iterator iter = nodes.iterator();
        while (iter.hasNext()) {
            JMeterTreeNode node = (JMeterTreeNode) iter.next();
            if (node.isEnabled()) {
                return node;
            }
        }
        return null;
    }

    private JMeterTreeNode findTargetControllerNode() {
        JMeterTreeNode myTarget = getTarget();
        if (myTarget != null) {
            return myTarget;
        }
        myTarget = findFirstNodeOfType(RecordingController.class);
        if (myTarget != null) {
            return myTarget;
        }
        myTarget = findFirstNodeOfType(ThreadGroup.class);
        if (myTarget != null) {
            return myTarget;
        }
        myTarget = findFirstNodeOfType(WorkBench.class);
        if (myTarget != null) {
            return myTarget;
        }
        log.error("Program error: proxy recording target not found.");
        return null;
    }

    private void placeSampler(RMISampler sampler, TestElement[] subConfigs,
                              JMeterTreeNode myTarget, MethodCallRecord record) {
        try {
            JMeterTreeModel treeModel = GuiPackage.getInstance().getTreeModel();

            boolean firstInBatch = false;
            long now = System.currentTimeMillis();
            // long deltaT = now - lastTime;

            // if(deltaT > sampleGap) {
            // }

            // if(lastTime = 0) {
            //     deltaT = 0;
            // }
            // lastTime = now;

            JMeterTreeNode newNode = treeModel.addComponent(sampler, myTarget);
            for (int i = 0; subConfigs != null && i < subConfigs.length; i++) {
                treeModel.addComponent(subConfigs[i], newNode);
            }

        } catch (IllegalUserActionException e) {
            JMeterUtils.reportErrorToUser(e.getMessage());
        }
    }

    public void bindProxy() {
        proxy = new NativeRmiProxy(getTargetRmiName());
        proxy.setServerPort(getProxyPort());
        proxy.setNamingPort(getProxyNamingPort());
        proxy.setBindingScript(getBindingScript());

        RmiSamplerGeneratorMethodRecorder recorder = new RmiSamplerGeneratorMethodRecorder();
        recorder.setTarget(this);
        proxy.setMethodRecorder(recorder);

        log.info("Starting proxy thread");

        proxy.start();
    }

    public void unbindProxy() {
        log.info("Stopping proxy thread");

        proxy.stop();
    }
}
