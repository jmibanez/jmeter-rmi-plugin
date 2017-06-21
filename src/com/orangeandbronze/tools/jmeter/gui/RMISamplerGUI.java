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
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import javax.swing.JCheckBox;

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

    private static final String TARGETNAME_FIELD = "targetName";
    private static final String METHODNAME_FIELD = "methodName";
    private static final String ARGUMENTS_SCRIPT_FIELD = "argumentsScript";

    private JTextField targetName;
    private JTextField methodName;
    private JCheckBox ignExceptions;

    private JSyntaxTextArea argsScript;
    private JTextScrollPane scroller;

    private RMISampler model;

    /**
     * Creates a new <code>RMISamplerGUI</code> instance.
     *
     */
    public RMISamplerGUI() {
        super();
        methodName = new JTextField("", 40);
        targetName = new JTextField("", 40);
        ignExceptions = new JCheckBox("Ignore Exceptions");
        argsScript = JSyntaxTextArea.getInstance(20, 20);
        scroller = JTextScrollPane.getInstance(argsScript, true);

        init();
    }

    @Override
    public void configure(TestElement e) {
        super.configure(e);
        model = (RMISampler) e;
        targetName.setText(model.getTargetName());
        methodName.setText(model.getMethodName());
        argsScript.setText(model.getArgumentsScript());
        ignExceptions.setSelected(model.isExceptionsIgnored());
    }

    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if(element instanceof RMISampler) {
            model = (RMISampler) element;
            model.setTargetName(targetName.getText());
            model.setMethodName(methodName.getText());
            model.setArgumentsScript(argsScript.getText());
            model.setExceptionsIgnored(ignExceptions.isSelected());
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
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        add(makeTitlePanel(), BorderLayout.NORTH);

        targetName.setName(TARGETNAME_FIELD);
        methodName.setName(METHODNAME_FIELD);

        JLabel targetNameLabel = new JLabel("Target name");
        targetNameLabel.setLabelFor(targetName);

        JLabel methodNameLabel = new JLabel("Method name");
        methodNameLabel.setLabelFor(methodName);

        Box b = Box.createHorizontalBox();
        b.add(targetNameLabel);
        b.add(targetName);

        Box b2 = Box.createHorizontalBox();
        b2.add(methodNameLabel);
        b2.add(methodName);
        b2.add(ignExceptions);

        Box targetBox = Box.createVerticalBox();
        targetBox.add(b);
        targetBox.add(b2);

        JLabel argLabel = new JLabel("Arguments script");
        argLabel.setLabelFor(scroller);
        argsScript.discardAllEdits();

        JPanel editorPanel = new VerticalPanel();
        editorPanel.add(argLabel, BorderLayout.NORTH);
        editorPanel.add(scroller, BorderLayout.CENTER);

        JPanel configPanel = new VerticalPanel();
        configPanel.add(targetBox, BorderLayout.NORTH);
        configPanel.add(editorPanel, BorderLayout.CENTER);

        add(configPanel, BorderLayout.CENTER);

    }
}

