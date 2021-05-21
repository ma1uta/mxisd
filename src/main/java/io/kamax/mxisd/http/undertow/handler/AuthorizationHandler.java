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

package io.kamax.mxisd.http.undertow.handler;

import io.kamax.mxisd.auth.AccountManager;
import io.kamax.mxisd.exception.InvalidCredentialsException;
import io.kamax.mxisd.storage.ormlite.dao.AccountDao;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationHandler extends BasicHttpHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationHandler.class);

    private final AccountManager accountManager;

    private final HttpHandler child;

    public static AuthorizationHandler around(AccountManager accountManager, HttpHandler child) {
        return new AuthorizationHandler(accountManager, child);
    }

    private AuthorizationHandler(AccountManager accountManager, HttpHandler child) {
        this.accountManager = accountManager;
        this.child = child;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String token = findAccessToken(exchange).orElse(null);
        if (token == null) {
            log.error("Unauthorized request from: {}", exchange.getHostAndPort());
            throw new InvalidCredentialsException();
        }

        synchronized (accountManager) {
            AccountDao account = accountManager.findAccount(token);
            if (account == null) {
                log.error("Account not found from request from: {}", exchange.getHostAndPort());
                throw new InvalidCredentialsException();
            }
            long expiredAt = (account.getCreatedAt() + account.getExpiresIn()) * 1000; // expired in milliseconds
            if (expiredAt < System.currentTimeMillis()) {
                log.error("Account for '{}' from: {}", account.getUserId(), exchange.getHostAndPort());
                accountManager.deleteAccount(token);
                throw new InvalidCredentialsException();
            }
            log.trace("Access for '{}' allowed", account.getUserId());
        }
        child.handleRequest(exchange);
    }
}
