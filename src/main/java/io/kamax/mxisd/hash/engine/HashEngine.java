package io.kamax.mxisd.hash.engine;

import io.kamax.mxisd.config.HashingConfig;
import io.kamax.mxisd.hash.storage.HashStorage;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;

public class HashEngine implements Engine {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashEngine.class);

    private final List<? extends IThreePidProvider> providers;
    private final HashStorage hashStorage;
    private final HashingConfig config;
    private final Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
    private String pepper;

    public HashEngine(List<? extends IThreePidProvider> providers, HashStorage hashStorage, HashingConfig config) {
        this.providers = providers;
        this.hashStorage = hashStorage;
        this.config = config;
    }

    @Override
    public void updateHashes() {
        LOGGER.info("Start update hashes.");
        synchronized (hashStorage) {
            this.pepper = newPepper();
            hashStorage.clear();
            for (IThreePidProvider provider : providers) {
                try {
                    LOGGER.info("Populate hashes from the handler: {}", provider.getClass().getCanonicalName());
                    for (ThreePidMapping pidMapping : provider.populateHashes()) {
                        LOGGER.debug("Found 3PID: {}", pidMapping);
                        hashStorage.add(pidMapping, hash(pidMapping));
                    }
                } catch (Exception e) {
                    LOGGER.error("Unable to update hashes of the provider: " + provider.toString(), e);
                }
            }
        }
        LOGGER.info("Finish update hashes.");
    }

    @Override
    public String getPepper() {
        synchronized (hashStorage) {
            return pepper;
        }
    }

    protected String hash(ThreePidMapping pidMapping) {
        return base64.encodeToString(DigestUtils.sha256(pidMapping.getValue() + " " + pidMapping.getMedium() + " " + getPepper()));
    }

    protected String newPepper() {
        return RandomStringUtils.random(config.getPepperLength(), true, true);
    }
}
