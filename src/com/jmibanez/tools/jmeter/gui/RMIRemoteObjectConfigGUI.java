/*
 *
 *
 * See README in the source tree for more info
 *
 */
 
package com.jmibanez.tools.jmeter.gui;

import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;
import java.awt.BorderLayout;
import javax.swing.Box;
import com.jmibanez.tools.jmeter.RMIRemoteObjectConfig;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.jmeter.gui.util.VerticalPanel;

/**
 * Describe class RMIRemoteObjectConfigGUI here.
 *
 *
 * Created: Fri Nov 14 14:11:24 2008
 *
 * @author <a href="mailto:jm@jmibanez.com">JM Ibanez</a>
 * @version 1.0
 */
public class RMIRemoteObjectConfigGUI extends AbstractConfigGui {

    public static final long serialVersionUID = 98030L;

    private static final String TARGETNAME_FIELD = "targetRmiName";

    private JTextField targetRmiName;

    private RMIRemoteObjectConfig model;

    /**
     * Creates a new <code>RMIRemoteObjectConfigGUI</code> instance.
     *
     */
    public RMIRemoteObjectConfigGUI() {
        super();
        init();
    }

    public String getLabelResource() {
        return "rmi_remote_object_config";
    }

    public String getStaticLabel() {
        return "RMI Remote Object config";
    }

    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if(element instanceof RMIRemoteObjectConfig) {
            model = (RMIRemoteObjectConfig) element;
            model.setTargetRmiName(targetRmiName.getText());
        }
    }

    public TestElement createTestElement() {
        RMIRemoteObjectConfig cfg = new RMIRemoteObjectConfig();
        modifyTestElement(cfg);
        return cfg;
    }

    @Override
    public void configure(TestElement e) {
        super.configure(e);
        model = (RMIRemoteObjectConfig) e;
        targetRmiName.setText(model.getTargetRmiName());
    }

    private void init() {
        setLayout(new BorderLayout());
        setBorder(makeBorder());

        add(makeTitlePanel(), BorderLayout.NORTH);

        Box config = Box.createVerticalBox();

        targetRmiName = new JTextField("", 40);
        targetRmiName.setName(TARGETNAME_FIELD);

        JLabel label = new JLabel("Target RMI name");
        label.setLabelFor(targetRmiName);

        config.add(label);
        config.add(targetRmiName);

        JPanel configPanel = new VerticalPanel();
        configPanel.add(config, BorderLayout.NORTH);

        add(configPanel, BorderLayout.CENTER);
    }
}
