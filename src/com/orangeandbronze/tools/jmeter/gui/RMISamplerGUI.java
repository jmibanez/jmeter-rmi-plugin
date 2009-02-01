package com.orangeandbronze.tools.jmeter.gui;

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.JPanel;

import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;

import com.orangeandbronze.tools.jmeter.RMISampler;
import javax.swing.JTextField;
import javax.swing.JLabel;
import org.apache.jmeter.gui.util.VerticalPanel;
import javax.swing.JTextArea;

/**
 * Describe class RMISamplerGUI here.
 *
 *
 * Created: Wed Nov 12 13:11:51 2008
 *
 * @author <a href="mailto:jm@orangeandbronze.com">JM Ibanez</a>
 * @version 1.0
 */
public class RMISamplerGUI extends AbstractSamplerGui {

    private static final String METHODNAME_FIELD = "methodName";
    private static final String ARGUMENTS_SCRIPT_FIELD = "argumentsScript";

    private JTextField methodName;
    private JTextArea argsScript;

    private RMISampler model;

    /**
     * Creates a new <code>RMISamplerGUI</code> instance.
     *
     */
    public RMISamplerGUI() {
        super();
        init();
    }

    @Override
    public void configure(TestElement e) {
        super.configure(e);
        model = (RMISampler) e;
        methodName.setText(model.getMethodName());
        argsScript.setText(model.getArgumentsScript());
    }

    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if(element instanceof RMISampler) {
            model = (RMISampler) element;
            model.setMethodName(methodName.getText());
            model.setArgumentsScript(argsScript.getText());
        }
    }

    public TestElement createTestElement() {
        RMISampler sampler = new RMISampler();
        modifyTestElement(sampler);
        return sampler;
    }

    public String getLabelResource() {
        return "rmi_sampler";
    }

    public String getStaticLabel() {
        return "RMI Sampler";
    }


    private void init() {
        setLayout(new BorderLayout());
        setBorder(makeBorder());

        add(makeTitlePanel(), BorderLayout.NORTH);

        methodName = new JTextField("", 40);
        methodName.setName(METHODNAME_FIELD);
        //targetRmiName.addKeyListener(this);

        JLabel label = new JLabel("Method name");
        label.setLabelFor(methodName);

        argsScript = new JTextArea("");
        argsScript.setName(ARGUMENTS_SCRIPT_FIELD);

        JLabel argLabel = new JLabel("Arguments script");
        argLabel.setLabelFor(argsScript);

        Box b = Box.createHorizontalBox();
        b.add(label);
        b.add(methodName);

        Box b2 = Box.createVerticalBox();
        b2.add(argLabel);
        b2.add(argsScript);

        JPanel configPanel = new VerticalPanel();
        configPanel.add(b, BorderLayout.NORTH);
        configPanel.add(b2, BorderLayout.CENTER);

        add(configPanel, BorderLayout.CENTER);
    }
}

