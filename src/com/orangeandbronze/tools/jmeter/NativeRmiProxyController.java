/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.orangeandbronze.tools.jmeter;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.RecordingController;
import org.apache.jmeter.testelement.WorkBench;
import org.apache.log.Logger;
//import org.apache.log4j.Logger;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.GuiPackage;
import java.util.Iterator;
import java.util.List;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.testelement.property.IntegerProperty;
import com.orangeandbronze.tools.jmeter.gui.NativeRmiProxyControllerGui;
import com.orangeandbronze.tools.jmeter.impl.RmiSamplerGeneratorMethodRecorder;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.modifiers.BeanShellPreProcessor;
import org.apache.jmeter.modifiers.BeanShellPreProcessorBeanInfo;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;

/**
 * Describe class NativeRmiProxyController here.
 *
 *
 * Created: Fri Nov 14 14:32:04 2008
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class NativeRmiProxyController extends GenericController {

    public static final String TARGET_RMI_NAME = "RmiProxy.target_rmi_name";
    public static final String PROXY_NAMING_PORT = "RmiProxy.proxy_naming_port";
    public static final String PROXY_PORT = "RmiProxy.proxy_port";

    private static Logger log = LoggingManager.getLoggerForClass(); // Logger.getLogger(NativeRmiProxyController.class);

    private JMeterTreeNode target;

    private NativeRmiProxy proxy;

    /**
     * Creates a new <code>NativeRmiProxyController</code> instance.
     *
     */
    public NativeRmiProxyController() {
    }


    public synchronized void deliverSampler(RMISampler s, MethodCallRecord record) {
        JMeterTreeNode myTarget = findTargetControllerNode();

        // Create bsh preprocessor node for arguments
        BeanShellPreProcessor argsPreProc = new BeanShellPreProcessor();
        argsPreProc.setProperty(TestElement.GUI_CLASS, TestBeanGUI.class.getName());
        argsPreProc.setName("Arguments for " + record.getMethod());

        TestElement[] subConfigs = new TestElement[] { argsPreProc };

        placeSampler(s, subConfigs, myTarget, record);
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


    public JMeterTreeNode getTarget() {
        return target;
    }

    public void setTarget(JMeterTreeNode target) {
        this.target = target;
    }

    private String createArgumentsScript(MethodCallRecord record) {
        log.info("Creating script for method call record");
        return "---";
    }


    public Class getGuiClass() {
        return com.orangeandbronze.tools.jmeter.gui.NativeRmiProxyControllerGui.class;
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
                if(subConfigs[i] instanceof BeanShellPreProcessor) {
                    BeanShellPreProcessor argsPreProc = (BeanShellPreProcessor) subConfigs[i];
                    argsPreProc.setFilename(null);
                    argsPreProc.setProperty("script", createArgumentsScript(record));
                }
            }

        } catch (IllegalUserActionException e) {
            JMeterUtils.reportErrorToUser(e.getMessage());
        }
    }

    public void bindProxy() {
        proxy = new NativeRmiProxy(getTargetRmiName());
        proxy.setServerPort(getProxyPort());
        proxy.setNamingPort(getProxyNamingPort());

        RmiSamplerGeneratorMethodRecorder recorder = new RmiSamplerGeneratorMethodRecorder();
        recorder.setTarget(this);
        proxy.setMethodRecorder(recorder);

        Thread t = new Thread(proxy);

        log.info("Starting proxy thread");

        t.start();
    }

    public void unbindProxy() {
        log.info("Stopping proxy thread");

        proxy.stop();
    }
}
