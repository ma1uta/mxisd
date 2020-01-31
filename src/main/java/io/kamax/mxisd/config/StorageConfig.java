/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

package io.kamax.mxisd.config;

import io.kamax.mxisd.exception.ConfigurationException;

public class StorageConfig {

    public enum BackendEnum {
        sqlite,

        postgresql
    }

    public static class Provider {

        private SQLiteStorageConfig sqlite = new SQLiteStorageConfig();

        private PostgresqlStorageConfig postgresql = new PostgresqlStorageConfig();

        public SQLiteStorageConfig getSqlite() {
            return sqlite;
        }

        public void setSqlite(SQLiteStorageConfig sqlite) {
            this.sqlite = sqlite;
        }

        public PostgresqlStorageConfig getPostgresql() {
            return postgresql;
        }

        public void setPostgresql(PostgresqlStorageConfig postgresql) {
            this.postgresql = postgresql;
        }
    }

    private BackendEnum backend = BackendEnum.sqlite; // or postgresql
    private Provider provider = new Provider();

    public BackendEnum getBackend() {
        return backend;
    }

    public void setBackend(BackendEnum backend) {
        this.backend = backend;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public void build() {
        if (getBackend() == null) {
            throw new ConfigurationException("storage.backend");
        }
    }

}
