package net.officefloor.hq.app.asof;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/**
 * What a sheet held as of a date. Each time the sheet's today changes, its rows are copied into a
 * schema of their own (SNAP_<id>, listed in sheet_snapshot). The sheet as of a date is what it held
 * at the last moment its today was on or before that date: the store as it is now while its today
 * still is, else the copy taken when its today last moved past that date, else nothing. A view
 * copies that into a schema the request reads (ASOF_<n>), with the date as the sheet's today, and
 * drops it after.
 *
 * Works on connections of its own, so a copy is never part of a request's transaction, and one copy
 * or drop at a time: H2 lets one session at a time change its schemas.
 */
@Component
public class AsOfStore {

    private static final String LIVE = "PUBLIC";

    /** Tables that are not the sheet's data. */
    private static final Set<String> SKIP = Set.of("flyway_schema_history", "SHEET_SNAPSHOT");

    private final DataSource dataSource;

    private final AtomicLong views = new AtomicLong();

    public AsOfStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Keep what the sheet holds now, before its today moves from {@code was} to {@code now}. */
    public synchronized void snapshot(String sheetId, LocalDate was, LocalDate now) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(true);
            long id;
            try (Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery("SELECT NEXT VALUE FOR sheet_snapshot_seq")) {
                rs.next();
                id = rs.getLong(1);
            }
            copy(c, LIVE, "SNAP_" + id, sheetId);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO sheet_snapshot (id, sheet_id, today_was, today_now) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, id);
                ps.setString(2, sheetId);
                ps.setObject(3, was);
                ps.setObject(4, now);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not keep the sheet as of its today", e);
        }
    }

    /** A new schema holding the sheet as of the date, with the date as its today. */
    public synchronized String view(String sheetId, LocalDate asOf) {
        String schema = "ASOF_" + views.incrementAndGet();
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(true);
            String[] sheet = liveSheet(c, sheetId);
            String source;
            if (sheet != null && !LocalDate.parse(sheet[1]).isAfter(asOf)) {
                source = LIVE;
            } else {
                source = null;
                try (PreparedStatement ps = c.prepareStatement("SELECT id FROM sheet_snapshot "
                        + "WHERE sheet_id = ? AND today_was <= ? AND today_now > ? ORDER BY id DESC LIMIT 1")) {
                    ps.setString(1, sheetId);
                    ps.setObject(2, asOf);
                    ps.setObject(3, asOf);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) source = "SNAP_" + rs.getLong(1);
                    }
                }
            }
            copy(c, source, schema, sheetId);
            if (source == null) {
                // Nothing on it yet: the sheet itself, as it was made.
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + q(schema) + ".SHEET "
                        + "(ID, CREATED_BY, CREATED_AT, TODAY, LAST_CLAIM_NUMBER, ARCHIVED) "
                        + "VALUES (?, ?, CURRENT_TIMESTAMP, ?, 0, FALSE)")) {
                    ps.setString(1, sheetId);
                    ps.setString(2, sheet == null ? "" : sheet[0]);
                    ps.setObject(3, asOf);
                    ps.executeUpdate();
                }
            }
            // The parent link is not dated: the view names the parent the sheet names now.
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM " + q(schema) + ".SHEET_SETTING WHERE SHEET_ID = ? AND SETTING = 'parent'")) {
                ps.setString(1, sheetId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + q(schema) + ".SHEET_SETTING SELECT * FROM "
                    + "PUBLIC.SHEET_SETTING WHERE SHEET_ID = ? AND SETTING = 'parent'")) {
                ps.setString(1, sheetId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE " + q(schema) + ".SHEET SET TODAY = ? WHERE ID = ?")) {
                ps.setObject(1, asOf);
                ps.setString(2, sheetId);
                ps.executeUpdate();
            }
            return schema;
        } catch (SQLException e) {
            drop(schema);
            throw new IllegalStateException("Could not show the sheet as of " + asOf, e);
        }
    }

    /** Drop a view's schema once its request is done. */
    public synchronized void drop(String schema) {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(true);
            s.execute("DROP SCHEMA IF EXISTS " + q(schema) + " CASCADE");
        } catch (SQLException e) {
            // left behind: it is never read again
        }
    }

    /** The live sheet's creator and today, or null when it was never loaded. */
    private static String[] liveSheet(Connection c, String sheetId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT created_by, today FROM PUBLIC.sheet WHERE id = ?")) {
            ps.setString(1, sheetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new String[] { rs.getString(1), rs.getObject(2, LocalDate.class).toString() } : null;
            }
        }
    }

    /** Copy the sheet's rows of every table from {@code source} (null: none) into a new schema. */
    private static void copy(Connection c, String source, String target, String sheetId) throws SQLException {
        List<String> tables = tables(c, LIVE);
        Set<String> inSource = source == null ? Set.of() : new HashSet<>(tables(c, source));
        String sheet = "'" + sheetId.replace("'", "''") + "'";
        try (Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + q(target));
            for (String table : tables) {
                boolean has = inSource.contains(table);
                String from = q(has ? source : LIVE);
                String where = has ? filter(c, table, from, sheet) : "FALSE";
                s.execute("CREATE TABLE " + q(target) + "." + q(table) + " AS SELECT * FROM " + from + "."
                        + q(table) + " WHERE " + where);
            }
        }
    }

    /** The condition picking the sheet's rows of a table. */
    private static String filter(Connection c, String table, String from, String sheet) throws SQLException {
        if (table.equals("SHEET")) return "ID = " + sheet;
        Set<String> columns = columns(c, table);
        if (columns.contains("SHEET_ID")) return "SHEET_ID = " + sheet;
        for (String owner : List.of("PERIOD", "PERSON", "CLAIM")) {
            if (columns.contains(owner + "_ID")) {
                return owner + "_ID IN (SELECT ID FROM " + from + "." + owner + " WHERE SHEET_ID = " + sheet + ")";
            }
        }
        return "TRUE";
    }

    private static List<String> tables(Connection c, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME")) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!SKIP.contains(rs.getString(1))) tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    private static Set<String> columns(Connection c, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?")) {
            ps.setString(1, LIVE);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) columns.add(rs.getString(1));
            }
        }
        return columns;
    }

    private static String q(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
