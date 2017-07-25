package com.jmibanez.tools.jmeter.util;

import java.rmi.Remote;
import com.jmibanez.tools.jmeter.MethodCallRecord;

public class InstanceHandleFactory {

    public static final String buildInstanceName(final Remote instance,
                                                 final MethodCallRecord record,
                                                 final String path) {
        String instancePath = path;
        if (instancePath == null || "".equals(instancePath)) {
            instancePath = "(return)";
        }
        return String.format("%1d %2s -> %3s",
                             record.getIndex(),
                             record.getMethod(),
                             instancePath);
    }
}
