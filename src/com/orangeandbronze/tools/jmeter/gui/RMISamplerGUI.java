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

    private JTextField methodName;

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
    }

    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if(element instanceof RMISampler) {
            model = (RMISampler) element;
            model.setMethodName(methodName.getText());
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

        Box b = Box.createHorizontalBox();
        b.add(label);
        b.add(methodName);

        JPanel configPanel = new VerticalPanel();
        configPanel.add(b, BorderLayout.NORTH);

        add(configPanel, BorderLayout.CENTER);
    }
}
