package com.orangeandbronze.tools.jmeter.util;

import java.rmi.Remote;

public class InstanceHandleFactory {

    public static final String buildInstanceName(Remote instance) {
        int stubHashCode = instance.hashCode();
        return "remote_I" + stubHashCode;
    }
}
