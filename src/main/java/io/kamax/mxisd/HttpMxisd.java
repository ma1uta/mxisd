/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd;

import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.config.PolicyConfig;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.http.undertow.handler.ApiHandler;
import io.kamax.mxisd.http.undertow.handler.AuthorizationHandler;
import io.kamax.mxisd.http.undertow.handler.CheckTermsHandler;
import io.kamax.mxisd.http.undertow.handler.InternalInfoHandler;
import io.kamax.mxisd.http.undertow.handler.OptionsHandler;
import io.kamax.mxisd.http.undertow.handler.RequestDumpingHandler;
import io.kamax.mxisd.http.undertow.handler.SaneHandler;
import io.kamax.mxisd.http.undertow.handler.as.v1.AsNotFoundHandler;
import io.kamax.mxisd.http.undertow.handler.as.v1.AsTransactionHandler;
import io.kamax.mxisd.http.undertow.handler.as.v1.AsUserHandler;
import io.kamax.mxisd.http.undertow.handler.auth.RestAuthHandler;
import io.kamax.mxisd.http.undertow.handler.auth.v1.LoginGetHandler;
import io.kamax.mxisd.http.undertow.handler.auth.v1.LoginHandler;
import io.kamax.mxisd.http.undertow.handler.auth.v1.LoginPostHandler;
import io.kamax.mxisd.http.undertow.handler.auth.v2.AccountGetUserInfoHandler;
import io.kamax.mxisd.http.undertow.handler.auth.v2.AccountLogoutHandler;
import io.kamax.mxisd.http.undertow.handler.auth.v2.AccountRegisterHandler;
import io.kamax.mxisd.http.undertow.handler.directory.v1.UserDirectorySearchHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.EphemeralKeyIsValidHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.HelloHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.KeyGetHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.RegularKeyIsValidHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.SessionStartHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.SessionTpidBindHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.SessionTpidGetValidatedHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.SessionTpidUnbindHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.SessionValidationGetHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.SessionValidationPostHandler;
import io.kamax.mxisd.http.undertow.handler.identity.share.SignEd25519Handler;
import io.kamax.mxisd.http.undertow.handler.identity.share.StoreInviteHandler;
import io.kamax.mxisd.http.undertow.handler.identity.v1.BulkLookupHandler;
import io.kamax.mxisd.http.undertow.handler.identity.v1.SingleLookupHandler;
import io.kamax.mxisd.http.undertow.handler.identity.v2.HashDetailsHandler;
import io.kamax.mxisd.http.undertow.handler.identity.v2.HashLookupHandler;
import io.kamax.mxisd.http.undertow.handler.internal.InternalInviteManagerHandler;
import io.kamax.mxisd.http.undertow.handler.invite.v1.RoomInviteHandler;
import io.kamax.mxisd.http.undertow.handler.profile.v1.InternalProfileHandler;
import io.kamax.mxisd.http.undertow.handler.profile.v1.ProfileHandler;
import io.kamax.mxisd.http.undertow.handler.register.v1.Register3pidRequestTokenHandler;
import io.kamax.mxisd.http.undertow.handler.status.StatusHandler;
import io.kamax.mxisd.http.undertow.handler.status.VersionHandler;
import io.kamax.mxisd.http.undertow.handler.term.v2.AcceptTermsHandler;
import io.kamax.mxisd.http.undertow.handler.term.v2.GetTermsHandler;
import io.kamax.mxisd.http.undertow.handler.versions.VersionsHandler;
import io.kamax.mxisd.matrix.IdentityServiceAPI;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class HttpMxisd {

    // Core
    private Mxisd m;

    // I/O
    private Undertow httpSrv;

    static {
        // Used in XNIO package, dependency of Undertow
        // We switch to slf4j
        System.setProperty("org.jboss.logging.provider", "slf4j");
    }

    public HttpMxisd(MxisdConfig cfg) {
        m = new Mxisd(cfg);
    }

    public void start() {
        m.start();

        HttpHandler asUserHandler = sane(new AsUserHandler(m.getAs()));
        HttpHandler asTxnHandler = sane(new AsTransactionHandler(m.getAs()));
        HttpHandler asNotFoundHandler = sane(new AsNotFoundHandler(m.getAs()));

        final RoutingHandler handler = Handlers.routing()
            .add("OPTIONS", "/**", sane(new OptionsHandler()))

            // Status endpoints
            .get(StatusHandler.Path, sane(new StatusHandler()))
            .get(VersionHandler.Path, sane(new VersionHandler()))

            // Authentication endpoints
            .get(LoginHandler.Path, sane(new LoginGetHandler(m.getAuth(), m.getHttpClient())))
            .post(LoginHandler.Path, sane(new LoginPostHandler(m.getAuth())))
            .post(RestAuthHandler.Path, sane(new RestAuthHandler(m.getAuth())))

            // Directory endpoints
            .post(UserDirectorySearchHandler.Path, sane(new UserDirectorySearchHandler(m.getDirectory())))

            // Profile endpoints
            .get(ProfileHandler.Path, sane(new ProfileHandler(m.getProfile())))
            .get(InternalProfileHandler.Path, sane(new InternalProfileHandler(m.getProfile())))

            // Registration endpoints
            .post(Register3pidRequestTokenHandler.Path,
                sane(new Register3pidRequestTokenHandler(m.getReg(), m.getClientDns(), m.getHttpClient())))

            // Invite endpoints
            .post(RoomInviteHandler.Path, sane(new RoomInviteHandler(m.getHttpClient(), m.getClientDns(), m.getInvite())))

            // Application Service endpoints
            .get(AsUserHandler.Path, asUserHandler)
            .get("/_matrix/app/v1/rooms/**", asNotFoundHandler)
            .put(AsTransactionHandler.Path, asTxnHandler)

            .get("/users/{" + AsUserHandler.ID + "}", asUserHandler) // Legacy endpoint
            .get("/rooms/**", asNotFoundHandler) // Legacy endpoint
            .put("/transactions/{" + AsTransactionHandler.ID + "}", asTxnHandler) // Legacy endpoint

            // Versions Endpoint
            .get(VersionsHandler.Path, sane(new VersionsHandler()))

            // Banned endpoints
            .get(InternalInfoHandler.Path, sane(new InternalInfoHandler()));
        keyEndpoints(handler);
        identityEndpoints(handler);
        termsEndpoints(handler);
        hashEndpoints(handler);
        accountEndpoints(handler);

        if (m.getConfig().getInternal().isEnabled()) {
            handler.get(InternalInviteManagerHandler.PATH, new InternalInviteManagerHandler(m.getInvite()));
        }

        ServerConfig serverConfig = m.getConfig().getServer();
        httpSrv = Undertow.builder().addHttpListener(serverConfig.getPort(), serverConfig.getHostname()).setHandler(handler).build();

        httpSrv.start();
    }

    public void stop() {
        // Because it might have never been initialized if an exception is thrown early
        if (Objects.nonNull(httpSrv)) {
            httpSrv.stop();
        }

        m.stop();
    }

    private void keyEndpoints(RoutingHandler routingHandler) {
        addEndpoints(routingHandler, Methods.GET, false,
            new KeyGetHandler(m.getKeyManager()),
            new RegularKeyIsValidHandler(m.getKeyManager()),
            new EphemeralKeyIsValidHandler(m.getKeyManager())
        );
    }

    private void identityEndpoints(RoutingHandler routingHandler) {
        // Legacy v1
        routingHandler.get(SingleLookupHandler.Path, sane(new SingleLookupHandler(m.getConfig(), m.getIdentity(), m.getSign())));
        routingHandler.post(BulkLookupHandler.Path, sane(new BulkLookupHandler(m.getIdentity())));

        addEndpoints(routingHandler, Methods.GET, false, new HelloHandler());

        addEndpoints(routingHandler, Methods.GET, true,
            new SessionValidationGetHandler(m.getSession(), m.getConfig()),
            new SessionTpidGetValidatedHandler(m.getSession())
        );
        addEndpoints(routingHandler, Methods.POST, true,
            new StoreInviteHandler(m.getConfig().getServer(), m.getInvite(), m.getKeyManager()),
            new SessionStartHandler(m.getSession()),
            new SessionValidationPostHandler(m.getSession()),
            new SessionTpidBindHandler(m.getSession(), m.getInvite(), m.getSign()),
            new SessionTpidUnbindHandler(m.getSession()),
            new SignEd25519Handler(m.getConfig(), m.getInvite(), m.getSign())
        );
    }

    private void accountEndpoints(RoutingHandler routingHandler) {
        MatrixConfig matrixConfig = m.getConfig().getMatrix();
        if (matrixConfig.isV2()) {
            routingHandler.post(AccountRegisterHandler.Path, sane(new AccountRegisterHandler(m.getAccMgr())));
            wrapWithTokenAndAuthorizationHandlers(routingHandler, Methods.GET, new AccountGetUserInfoHandler(m.getAccMgr()),
                AccountGetUserInfoHandler.Path, true);
            wrapWithTokenAndAuthorizationHandlers(routingHandler, Methods.GET, new AccountLogoutHandler(m.getAccMgr()),
                AccountLogoutHandler.Path, true);
        }
    }

    private void termsEndpoints(RoutingHandler routingHandler) {
        MatrixConfig matrixConfig = m.getConfig().getMatrix();
        if (matrixConfig.isV2()) {
            routingHandler.get(GetTermsHandler.PATH, sane(new GetTermsHandler(m.getConfig().getPolicy())));
            routingHandler.post(AcceptTermsHandler.PATH, sane(new AcceptTermsHandler(m.getAccMgr())));
        }
    }

    private void hashEndpoints(RoutingHandler routingHandler) {
        MatrixConfig matrixConfig = m.getConfig().getMatrix();
        if (matrixConfig.isV2()) {
            wrapWithTokenAndAuthorizationHandlers(routingHandler, Methods.GET, new HashDetailsHandler(m.getHashManager()),
                HashDetailsHandler.PATH, true);
            wrapWithTokenAndAuthorizationHandlers(routingHandler, Methods.POST,
                new HashLookupHandler(m.getIdentity(), m.getHashManager()), HashLookupHandler.Path, true);
        }
    }

    private void addEndpoints(RoutingHandler routingHandler, HttpString method, boolean useAuthorization, ApiHandler... handlers) {
        for (ApiHandler handler : handlers) {
            attachHandler(routingHandler, method, handler, useAuthorization, sane(handler));
        }
    }

    private void attachHandler(RoutingHandler routingHandler, HttpString method, ApiHandler apiHandler, boolean useAuthorization,
                               HttpHandler httpHandler) {
        MatrixConfig matrixConfig = m.getConfig().getMatrix();
        if (matrixConfig.isV1()) {
            routingHandler.add(method, apiHandler.getPath(IdentityServiceAPI.V1), sane(httpHandler));
        }
        if (matrixConfig.isV2()) {
            wrapWithTokenAndAuthorizationHandlers(routingHandler, method, httpHandler, apiHandler.getPath(IdentityServiceAPI.V2),
                useAuthorization);
        }
    }

    private void wrapWithTokenAndAuthorizationHandlers(RoutingHandler routingHandler, HttpString method, HttpHandler httpHandler,
                                                       String url, boolean useAuthorization) {
        List<PolicyConfig.PolicyObject> policyObjects = getPolicyObjects(url);
        HttpHandler wrappedHandler;
        if (useAuthorization) {
            wrappedHandler = policyObjects.isEmpty() ? httpHandler : CheckTermsHandler.around(m.getAccMgr(), httpHandler, policyObjects);
            wrappedHandler = AuthorizationHandler.around(m.getAccMgr(), wrappedHandler);
        } else {
            wrappedHandler = httpHandler;
        }
        routingHandler.add(method, url, sane(wrappedHandler));
    }

    @NotNull
    private List<PolicyConfig.PolicyObject> getPolicyObjects(String url) {
        PolicyConfig policyConfig = m.getConfig().getPolicy();
        List<PolicyConfig.PolicyObject> policies = new ArrayList<>();
        if (!policyConfig.getPolicies().isEmpty()) {
            for (PolicyConfig.PolicyObject policy : policyConfig.getPolicies().values()) {
                for (Pattern pattern : policy.getPatterns()) {
                    if (pattern.matcher(url).matches()) {
                        policies.add(policy);
                    }
                }
            }
        }
        return policies;
    }

    private HttpHandler sane(HttpHandler httpHandler) {
        SaneHandler handler = SaneHandler.around(httpHandler);
        if (m.getConfig().getLogging().isRequests()) {
            return new RequestDumpingHandler(handler);
        } else {
            return handler;
        }
    }
}
