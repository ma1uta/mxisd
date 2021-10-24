package io.kamax.mxisd.http.undertow.handler.versions;

import com.google.gson.JsonObject;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.undertow.server.HttpServerExchange;


public class VersionsHandler extends BasicHttpHandler {

    public static final String Path = "/_matrix/identity/versions";

    public void handleRequest(HttpServerExchange exchange) {
        JsonObject versions = new JsonObject();
        versions.addProperty("versions", "[\"r0.3.0\"]");

        JsonObject obj = new JsonObject();
        obj.add("versions", versions);

        respond(exchange, obj);
    }
}
