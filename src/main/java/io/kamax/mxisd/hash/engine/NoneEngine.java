package io.kamax.mxisd.hash.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoneEngine implements Engine {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoneEngine.class);

    @Override
    public void updateHashes() {
        LOGGER.info("Nothing to update.");
    }

    @Override
    public String getPepper() {
        return "";
    }
}
