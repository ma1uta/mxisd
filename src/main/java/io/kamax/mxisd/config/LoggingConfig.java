package io.kamax.mxisd.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingConfig.class);

    private String root;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public void build() {
        String systemLevel = System.getProperty("org.slf4j.simpleLogger.log.io.kamax.mxisd");
        LOGGER.info("Logging config:");
        if (StringUtils.isNotBlank(systemLevel)) {
            LOGGER.info("  Logging level set by environment: {}", systemLevel);
        } else if (StringUtils.isNotBlank(getRoot())) {
            LOGGER.info("  Logging level set by the configuration: {}", getRoot());
            System.setProperty("org.slf4j.simpleLogger.log.io.kamax.mxisd", getRoot());
        } else {
            LOGGER.info("  Logging level hasn't set, use default");
        }
    }
}
