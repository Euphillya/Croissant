package fr.euphyllia.tenseimc.storage.database.postgres.util;

import fr.euphyllia.tenseimc.storage.database.postgres.PostgresManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class SQLExecute {

    private SQLExecute() {
    }

    public static void query(
            @NotNull final String operation,
            @NotNull final String sql,
            @Nullable final List<?> params,
            @NotNull final Consumer<ResultSet> consumer
    ) {
        runQuery(PostgresManager.saveDataSource(), operation, sql, params, consumer);
    }

    public static <T> @Nullable T queryMap(
            @NotNull final String operation,
            @NotNull final String sql,
            @Nullable final List<?> params,
            @NotNull final Function<ResultSet, T> mapper
    ) {
        return runQueryMap(PostgresManager.saveDataSource(), operation, sql, params, mapper);
    }

    public static int update(
            @NotNull final String operation,
            @NotNull final String sql,
            @Nullable final List<?> params
    ) {
        return runUpdate(PostgresManager.saveDataSource(), operation, sql, params);
    }

    public static void work(@NotNull final String operation, @NotNull final SQLWork work) {
        runWork(PostgresManager.saveDataSource(), operation, work);
    }

    public static void transaction(@NotNull final String operation, @NotNull final SQLWork work) {
        runTransaction(PostgresManager.saveDataSource(), operation, work);
    }


    public static <T> @Nullable T queryMapOnLogin(
            @NotNull final String operation,
            @NotNull final String sql,
            @Nullable final List<?> params,
            @NotNull final Function<ResultSet, T> mapper
    ) {
        return runQueryMap(PostgresManager.loginDataSource(), operation, sql, params, mapper);
    }

    public static int updateOnLogin(
            @NotNull final String operation,
            @NotNull final String sql,
            @Nullable final List<?> params
    ) {
        return runUpdate(PostgresManager.loginDataSource(), operation, sql, params);
    }

    public static void workOn(
            @NotNull final DataSource dataSource,
            @NotNull final String operation,
            @NotNull final SQLWork work
    ) {
        runWork(dataSource, operation, work);
    }

    public static int updateOn(
            @NotNull final DataSource dataSource,
            @NotNull final String operation,
            @NotNull final String sql,
            @Nullable final List<?> params
    ) {
        return runUpdate(dataSource, operation, sql, params);
    }

    private static void runQuery(
            final DataSource ds, final String operation, final String sql,
            final List<?> params, final Consumer<ResultSet> consumer
    ) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParams(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                consumer.accept(rs);
            }
        } catch (final SQLException ex) {
            throw SqlExceptionTranslator.translate(operation, ex);
        }
    }

    private static <T> T runQueryMap(
            final DataSource ds, final String operation, final String sql,
            final List<?> params, final Function<ResultSet, T> mapper
    ) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParams(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapper.apply(rs);
            }
        } catch (final SQLException ex) {
            throw SqlExceptionTranslator.translate(operation, ex);
        }
    }

    private static int runUpdate(
            final DataSource ds, final String operation, final String sql, final List<?> params
    ) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParams(stmt, params);
            return stmt.executeUpdate();
        } catch (final SQLException ex) {
            throw SqlExceptionTranslator.translate(operation, ex);
        }
    }

    private static void runWork(final DataSource ds, final String operation, final SQLWork work) {
        try (Connection conn = ds.getConnection()) {
            work.run(conn);
        } catch (final SQLException ex) {
            throw SqlExceptionTranslator.translate(operation, ex);
        }
    }

    private static void runTransaction(final DataSource ds, final String operation, final SQLWork work) {
        try (Connection conn = ds.getConnection()) {
            final boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                work.run(conn);
                conn.commit();
            } catch (final Exception inner) {
                try {
                    conn.rollback();
                } catch (final SQLException rollbackEx) {
                    inner.addSuppressed(rollbackEx);
                }
                if (inner instanceof SQLException sqlEx) {
                    throw SqlExceptionTranslator.translate(operation, sqlEx);
                }
                if (inner instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Transaction failed: " + operation, inner);
            } finally {
                try {
                    conn.setAutoCommit(oldAutoCommit);
                } catch (final SQLException ignored) {
                    // best-effort restore
                }
            }
        } catch (final SQLException ex) {
            throw SqlExceptionTranslator.translate(operation, ex);
        }
    }

    private static void bindParams(final PreparedStatement stmt, @Nullable final List<?> params) throws SQLException {
        if (params == null || params.isEmpty()) return;
        int idx = 1;
        for (final Object p : params) {
            stmt.setObject(idx++, p);
        }
    }

    @FunctionalInterface
    public interface SQLWork {
        void run(Connection connection) throws SQLException;
    }
}
