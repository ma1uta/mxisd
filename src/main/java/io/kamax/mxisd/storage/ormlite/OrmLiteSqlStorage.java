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

package io.kamax.mxisd.storage.ormlite;

import com.j256.ormlite.dao.CloseableWrappedIterable;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import io.kamax.matrix.ThreePid;
import io.kamax.mxisd.config.PolicyConfig;
import io.kamax.mxisd.config.StorageConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.exception.InvalidCredentialsException;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.dao.IThreePidSessionDao;
import io.kamax.mxisd.storage.ormlite.dao.ASTransactionDao;
import io.kamax.mxisd.storage.ormlite.dao.AccountDao;
import io.kamax.mxisd.storage.ormlite.dao.ChangelogDao;
import io.kamax.mxisd.storage.ormlite.dao.HashDao;
import io.kamax.mxisd.storage.ormlite.dao.HistoricalThreePidInviteIO;
import io.kamax.mxisd.storage.ormlite.dao.AcceptedDao;
import io.kamax.mxisd.storage.ormlite.dao.ThreePidInviteIO;
import io.kamax.mxisd.storage.ormlite.dao.ThreePidSessionDao;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrmLiteSqlStorage implements IStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrmLiteSqlStorage.class);

    @FunctionalInterface
    private interface Getter<T> {

        T get() throws SQLException, IOException;

    }

    @FunctionalInterface
    private interface Doer {

        void run() throws SQLException, IOException;

    }

    public static class Migrations {
        public static final String FIX_ACCEPTED_DAO = "2019_12_09__2254__fix_accepted_dao";
    }

    private Dao<ThreePidInviteIO, String> invDao;
    private Dao<HistoricalThreePidInviteIO, String> expInvDao;
    private Dao<ThreePidSessionDao, String> sessionDao;
    private Dao<ASTransactionDao, String> asTxnDao;
    private Dao<AccountDao, String> accountDao;
    private Dao<AcceptedDao, Long> acceptedDao;
    private Dao<HashDao, String> hashDao;
    private Dao<ChangelogDao, String> changelogDao;
    private StorageConfig.BackendEnum backend;

    public OrmLiteSqlStorage(StorageConfig.BackendEnum backend, String path) {
        this(backend, path, null, null);
    }

    public OrmLiteSqlStorage(StorageConfig.BackendEnum backend, String database, String username, String password) {
        if (backend == null) {
            throw new ConfigurationException("storage.backend");
        }
        this.backend = backend;

        if (StringUtils.isBlank(database)) {
            throw new ConfigurationException("Storage destination cannot be empty");
        }

        withCatcher(() -> {
            ConnectionSource connPool = new JdbcConnectionSource("jdbc:" + backend + ":" + database, username, password);
            changelogDao = createDaoAndTable(connPool, ChangelogDao.class);
            invDao = createDaoAndTable(connPool, ThreePidInviteIO.class);
            expInvDao = createDaoAndTable(connPool, HistoricalThreePidInviteIO.class);
            sessionDao = createDaoAndTable(connPool, ThreePidSessionDao.class);
            asTxnDao = createDaoAndTable(connPool, ASTransactionDao.class);
            accountDao = createDaoAndTable(connPool, AccountDao.class);
            acceptedDao = createDaoAndTable(connPool, AcceptedDao.class, true);
            hashDao = createDaoAndTable(connPool, HashDao.class);
            runMigration(connPool);
        });
    }

    private void runMigration(ConnectionSource connPol) throws SQLException {
        ChangelogDao fixAcceptedDao = changelogDao.queryForId(Migrations.FIX_ACCEPTED_DAO);
        if (fixAcceptedDao == null) {
            fixAcceptedDao(connPol);
            changelogDao.create(new ChangelogDao(Migrations.FIX_ACCEPTED_DAO, new Date(), "Recreate the accepted table."));
        }
    }

    private void fixAcceptedDao(ConnectionSource connPool) throws SQLException {
        LOGGER.info("Migration: {}", Migrations.FIX_ACCEPTED_DAO);
        TableUtils.dropTable(acceptedDao, true);
        TableUtils.createTableIfNotExists(connPool, AcceptedDao.class);
    }

    private <V, K> Dao<V, K> createDaoAndTable(ConnectionSource connPool, Class<V> c) throws SQLException {
        return createDaoAndTable(connPool, c, false);
    }

    /**
     * Workaround for https://github.com/j256/ormlite-core/issues/20.
     */
    private <V, K> Dao<V, K> createDaoAndTable(ConnectionSource connPool, Class<V> c, boolean workaround) throws SQLException {
        LOGGER.info("Create the dao: {}", c.getSimpleName());
        Dao<V, K> dao = DaoManager.createDao(connPool, c);
        if (workaround && StorageConfig.BackendEnum.postgresql.equals(backend)) {
            LOGGER.info("Workaround for postgresql on dao: {}", c.getSimpleName());
            try {
                dao.countOf();
                LOGGER.info("Table exists, do nothing");
            } catch (SQLException e) {
                LOGGER.info("Table doesn't exist, create");
                TableUtils.createTableIfNotExists(connPool, c);
            }
        } else {
            TableUtils.createTableIfNotExists(connPool, c);
        }
        return dao;
    }

    private <T> T withCatcher(Getter<T> g) {
        try {
            return g.get();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e); // FIXME do better
        }
    }

    private void withCatcher(Doer d) {
        try {
            d.run();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e); // FIXME do better
        }
    }

    private <T> List<T> forIterable(CloseableWrappedIterable<? extends T> t) {
        return withCatcher(() -> {
            try {
                List<T> ioList = new ArrayList<>();
                t.forEach(ioList::add);
                return ioList;
            } finally {
                t.close();
            }
        });
    }

    @Override
    public Collection<ThreePidInviteIO> getInvites() {
        return forIterable(invDao.getWrappedIterable());
    }

    @Override
    public void insertInvite(IThreePidInviteReply data) {
        withCatcher(() -> {
            int updated = invDao.create(new ThreePidInviteIO(data));
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

    @Override
    public void deleteInvite(String id) {
        withCatcher(() -> {
            int updated = invDao.deleteById(id);
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

    @Override
    public void insertHistoricalInvite(IThreePidInviteReply data, String resolvedTo, Instant resolvedAt, boolean couldPublish) {
        withCatcher(() -> {
            HistoricalThreePidInviteIO io = new HistoricalThreePidInviteIO(data, resolvedTo, resolvedAt, couldPublish);
            int updated = expInvDao.create(io);
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }

            // Ugly, but it avoids touching the structure of the historical parent class
            // and avoid any possible regression at this point.
            updated = expInvDao.updateId(io, UUID.randomUUID().toString().replace("-", ""));
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

    @Override
    public Optional<IThreePidSessionDao> getThreePidSession(String sid) {
        return withCatcher(() -> Optional.ofNullable(sessionDao.queryForId(sid)));
    }

    @Override
    public Optional<IThreePidSessionDao> findThreePidSession(ThreePid tpid, String secret) {
        return withCatcher(() -> {
            List<ThreePidSessionDao> daoList = sessionDao.queryForMatchingArgs(new ThreePidSessionDao(tpid, secret));
            if (daoList.size() > 1) {
                throw new InternalServerError("Lookup for 3PID Session " +
                    tpid + " returned more than one result");
            }

            if (daoList.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(daoList.get(0));
        });
    }

    @Override
    public void insertThreePidSession(IThreePidSessionDao session) {
        withCatcher(() -> {
            int updated = sessionDao.create(new ThreePidSessionDao(session));
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

    @Override
    public void updateThreePidSession(IThreePidSessionDao session) {
        withCatcher(() -> {
            int updated = sessionDao.update(new ThreePidSessionDao(session));
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

    @Override
    public void insertTransactionResult(String localpart, String txnId, Instant completion, String result) {
        withCatcher(() -> {
            int created = asTxnDao.create(new ASTransactionDao(localpart, txnId, completion, result));
            if (created != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + created);
            }
        });
    }

    @Override
    public Optional<ASTransactionDao> getTransactionResult(String localpart, String txnId) {
        return withCatcher(() -> {
            ASTransactionDao dao = new ASTransactionDao();
            dao.setLocalpart(localpart);
            dao.setTransactionId(txnId);
            List<ASTransactionDao> daoList = asTxnDao.queryForMatchingArgs(dao);

            if (daoList.size() > 1) {
                throw new InternalServerError("Lookup for Transaction " +
                    txnId + " for localpart " + localpart + " returned more than one result");
            }

            if (daoList.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(daoList.get(0));
        });
    }

    @Override
    public void insertToken(AccountDao account) {
        withCatcher(() -> {
            int created = accountDao.create(account);
            if (created != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + created);
            }
        });
    }

    @Override
    public Optional<AccountDao> findAccount(String token) {
        return withCatcher(() -> {
            List<AccountDao> accounts = accountDao.queryForEq("token", token);
            if (accounts.isEmpty()) {
                return Optional.empty();
            }
            if (accounts.size() != 1) {
                throw new RuntimeException("Unexpected rows for access token: " + accounts.size());
            }
            return Optional.of(accounts.get(0));
        });
    }

    @Override
    public void deleteToken(String token) {
        withCatcher(() -> {
            int updated = accountDao.deleteById(token);
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

    @Override
    public void acceptTerm(String token, String url) {
        withCatcher(() -> {
            AccountDao account = findAccount(token).orElseThrow(InvalidCredentialsException::new);
            List<AcceptedDao> acceptedTerms = acceptedDao.queryForEq("userId", account.getUserId());
            for (AcceptedDao acceptedTerm : acceptedTerms) {
                if (acceptedTerm.getUrl().equalsIgnoreCase(url)) {
                    // already accepted
                    return;
                }
            }
            int created = acceptedDao.create(new AcceptedDao(url, account.getUserId(), System.currentTimeMillis()));
            if (created != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + created);
            }
        });
    }

    @Override
    public void deleteAccepts(String token) {
        withCatcher(() -> {
            AccountDao account = findAccount(token).orElseThrow(InvalidCredentialsException::new);
            acceptedDao.delete(acceptedDao.queryForEq("userId", account.getUserId()));
        });
    }

    @Override
    public boolean isTermAccepted(String token, List<PolicyConfig.PolicyObject> policies) {
        return withCatcher(() -> {
            AccountDao account = findAccount(token).orElseThrow(InvalidCredentialsException::new);
            List<AcceptedDao> acceptedTerms = acceptedDao.queryForEq("userId", account.getUserId());
            for (AcceptedDao acceptedTerm : acceptedTerms) {
                for (PolicyConfig.PolicyObject policy : policies) {
                    for (PolicyConfig.TermObject termObject : policy.getTerms().values()) {
                        if (termObject.getUrl().equalsIgnoreCase(acceptedTerm.getUrl())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        });
    }

    @Override
    public void clearHashes() {
        withCatcher(() -> {
            List<HashDao> allHashes = hashDao.queryForAll();
            int deleted = hashDao.delete(allHashes);
            if (deleted != allHashes.size()) {
                throw new RuntimeException("Not all hashes deleted: " + deleted);
            }
        });
    }

    @Override
    public void addHash(String mxid, String medium, String address, String hash) {
        withCatcher(() -> {
            hashDao.create(new HashDao(mxid, medium, address, hash));
        });
    }

    @Override
    public Collection<Pair<String, ThreePidMapping>> findHashes(Iterable<String> hashes) {
        return withCatcher(() -> {
            QueryBuilder<HashDao, String> builder = hashDao.queryBuilder();
            builder.where().in("hash", hashes);
            return hashDao.query(builder.prepare()).stream()
                .map(dao -> Pair.of(dao.getHash(), new ThreePidMapping(dao.getMedium(), dao.getAddress(), dao.getMxid()))).collect(
                    Collectors.toList());
        });
    }
}
