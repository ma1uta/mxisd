package io.kamax.mxisd.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("App");

    private String root;
    private String app;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public void build() {
        LOGGER.info("Logging config:");
        if (StringUtils.isNotBlank(getRoot())) {
            LOGGER.info("  Default log level: {}", getRoot());
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", getRoot());
        }

        String appLevel = System.getProperty("org.slf4j.simpleLogger.log.io.kamax.mxisd");
        if (StringUtils.isNotBlank(appLevel)) {
            LOGGER.info("  Logging level set by environment: {}", appLevel);
        } else if (StringUtils.isNotBlank(getApp())) {
            System.setProperty("org.slf4j.simpleLogger.log.io.kamax.mxisd", getApp());
            LOGGER.info("  Logging level set by the configuration: {}", getApp());
        } else {
            LOGGER.info("  Logging level hasn't set, use default");
        }
    }
}
