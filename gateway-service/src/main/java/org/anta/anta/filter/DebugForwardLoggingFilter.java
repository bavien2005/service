package org.anta.anta.filter;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DebugForwardLoggingFilter {

    private static final Logger LOG = Logger.getLogger(DebugForwardLoggingFilter.class);

    public void logForward(String incomingPath, String willForwardTo) {
        LOG.infof("[GATEWAY DEBUG] incoming=%s, willForwardTo=%s", incomingPath, willForwardTo);
    }
}