package io.kamax.mxisd.http.undertow.handler.identity.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.mxisd.config.HashingConfig;
import io.kamax.mxisd.hash.HashManager;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.undertow.server.HttpServerExchange;

public class HashDetailsHandler extends BasicHttpHandler {

    public static final String PATH = "/_matrix/identity/v2/hash_details";

    private final HashManager hashManager;

    public HashDetailsHandler(HashManager hashManager) {
        this.hashManager = hashManager;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        respond(exchange, getResponse());
    }

    private JsonObject getResponse() {
        JsonObject response = new JsonObject();
        response.addProperty("lookup_pepper", hashManager.getHashEngine().getPepper());
        JsonArray algorithms = new JsonArray();
        HashingConfig config = hashManager.getConfig();
        if (config.isEnabled()) {
            for (HashingConfig.Algorithm algorithm : config.getAlgorithms()) {
                algorithms.add(algorithm.name().toLowerCase());
            }
        } else {
            algorithms.add(HashingConfig.Algorithm.none.name().toLowerCase());
        }
        response.add("algorithms", algorithms);
        return response;
    }
}
