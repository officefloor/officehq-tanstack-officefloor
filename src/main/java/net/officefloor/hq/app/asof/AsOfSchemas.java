package net.officefloor.hq.app.asof;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import net.officefloor.hq.app.rate.RateRepository;
import org.springframework.stereotype.Component;

/**
 * The schemas a sheet is read from as of a date. Each holds every table the app keeps (under its own name, so every
 * repository reads it unchanged once a request's connection is switched to it), with {@code sheet} a view whose today
 * is the session's {@code @asof} date:
 * <ul>
 * <li>a snapshot: a copy of the sheet's rows (and its pools' members, and the rates of the sheets above it it
 * inherits rates from) as they stood at a moment;</li>
 * <li>{@link #LIVE}: the sheet as it stands now;</li>
 * <li>{@link #EMPTY}: nothing on the sheet yet (every setting at its default).</li>
 * </ul>
 * Schemas are made on connections of their own (H2 commits DDL), never on a request's transaction.
 */
@Component
public class AsOfSchemas {

    public static final String LIVE = "ASOF_LIVE";
    public static final String EMPTY = "ASOF_EMPTY";

    private static final String SHEET = "SHEET";
    private static final List<String> NOT_COPIED = List.of("SHEET_DAY", "flyway_schema_history");
    /** The tables also copied for the sheets above it, so the rates it inherits read as they stood. */
    private static final List<String> INHERITED = List.of("RATE", "RATE_VERSION");

    private final DataSource dataSource;
    private boolean shared;

    public AsOfSchemas(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Copy every row kept for the sheet, as committed now, into a new schema; its name. */
    public String snapshot(String sheetId) {
        String schema = "ASOF_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String id = literal(sheetId);
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(true);
            String inherited = String.join(", ", lineage(c, sheetId).stream().map(AsOfSchemas::literal).toList());
            s.execute("CREATE SCHEMA " + schema);
            for (String table : tables(c)) {
                s.execute("CREATE TABLE " + schema + "." + q(table) + " AS SELECT * FROM PUBLIC." + q(table)
                        + " WHERE SHEET_ID " + (INHERITED.contains(table) ? "IN (" + inherited + ")" : "= " + id));
            }
            for (String table : poolTables(c)) {
                s.execute("CREATE TABLE " + schema + "." + q(table) + " AS SELECT * FROM PUBLIC." + q(table)
                        + " WHERE POOL_ID IN (SELECT ID FROM PUBLIC.POOL WHERE SHEET_ID = " + id + ")");
            }
            s.execute("CREATE TABLE " + schema + ".SHEET_DATA AS SELECT * FROM PUBLIC.SHEET WHERE ID = " + id);
            s.execute("CREATE VIEW " + schema + ".SHEET AS SELECT " + sheetColumns(c, false) + " FROM " + schema
                    + ".SHEET_DATA s");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not keep sheet " + sheetId + " as it stands", e);
        }
        return schema;
    }

    /** The sheet and the sheets above it it inherits rates from: see {@link RateRepository#findAll}. */
    private static List<String> lineage(Connection c, String sheetId) throws SQLException {
        List<String> ids = new ArrayList<>(List.of(sheetId));
        try (PreparedStatement p = c.prepareStatement("SELECT PARENT_ID FROM PUBLIC.SHEET WHERE ID = ?")) {
            String at = sheetId;
            for (int level = 1; level <= RateRepository.PARENT_LEVELS && at != null; level++) {
                p.setString(1, at);
                String up = null;
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) {
                        up = rs.getString(1);
                    }
                }
                if (up == null || ids.contains(up)) {
                    break;
                }
                ids.add(up);
                at = up;
            }
        }
        return ids;
    }

    private static String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    /** Make the live and empty schemas, once. */
    public synchronized void ensureShared() {
        if (shared) {
            return;
        }
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(true);
            s.execute("DROP SCHEMA IF EXISTS " + LIVE + " CASCADE");
            s.execute("DROP SCHEMA IF EXISTS " + EMPTY + " CASCADE");
            s.execute("CREATE SCHEMA " + LIVE);
            s.execute("CREATE SCHEMA " + EMPTY);
            for (String table : tables(c)) {
                s.execute("CREATE VIEW " + LIVE + "." + q(table) + " AS SELECT * FROM PUBLIC." + q(table));
                s.execute("CREATE TABLE " + EMPTY + "." + q(table) + " AS SELECT * FROM PUBLIC." + q(table)
                        + " WITH NO DATA");
            }
            for (String table : poolTables(c)) {
                s.execute("CREATE VIEW " + LIVE + "." + q(table) + " AS SELECT * FROM PUBLIC." + q(table));
                s.execute("CREATE TABLE " + EMPTY + "." + q(table) + " AS SELECT * FROM PUBLIC." + q(table)
                        + " WITH NO DATA");
            }
            s.execute("CREATE VIEW " + LIVE + ".SHEET AS SELECT " + sheetColumns(c, false) + " FROM PUBLIC.SHEET s");
            s.execute("CREATE VIEW " + EMPTY + ".SHEET AS SELECT " + sheetColumns(c, true) + " FROM PUBLIC.SHEET s");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not make the as-of schemas", e);
        }
        shared = true;
    }

    /** Every table kept per sheet (each has a SHEET_ID column), other than the sheet itself. */
    private static List<String> tables(Connection c) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT t.TABLE_NAME FROM INFORMATION_SCHEMA.TABLES t"
                + " WHERE t.TABLE_SCHEMA = 'PUBLIC' AND t.TABLE_TYPE = 'BASE TABLE' AND EXISTS (SELECT 1 FROM"
                + " INFORMATION_SCHEMA.COLUMNS c WHERE c.TABLE_SCHEMA = 'PUBLIC' AND c.TABLE_NAME = t.TABLE_NAME"
                + " AND c.COLUMN_NAME = 'SHEET_ID') ORDER BY t.TABLE_NAME"); ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                String table = rs.getString(1);
                if (!NOT_COPIED.contains(table) && !SHEET.equals(table)) {
                    tables.add(table);
                }
            }
        }
        return tables;
    }

    /** Every table kept per pool (each has a POOL_ID column but no SHEET_ID one): kept with the sheet's pools. */
    private static List<String> poolTables(Connection c) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT t.TABLE_NAME FROM INFORMATION_SCHEMA.TABLES t"
                + " WHERE t.TABLE_SCHEMA = 'PUBLIC' AND t.TABLE_TYPE = 'BASE TABLE' AND EXISTS (SELECT 1 FROM"
                + " INFORMATION_SCHEMA.COLUMNS c WHERE c.TABLE_SCHEMA = 'PUBLIC' AND c.TABLE_NAME = t.TABLE_NAME"
                + " AND c.COLUMN_NAME = 'POOL_ID') AND NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS c"
                + " WHERE c.TABLE_SCHEMA = 'PUBLIC' AND c.TABLE_NAME = t.TABLE_NAME AND c.COLUMN_NAME = 'SHEET_ID')"
                + " ORDER BY t.TABLE_NAME"); ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    /**
     * The sheet's columns, read from {@code s}, with its today the session's {@code @asof} date; {@code defaults}
     * puts every other column but its id at its default, as on a sheet nothing was done to yet.
     */
    private static String sheetColumns(Connection c, boolean defaults) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT COLUMN_NAME, COLUMN_DEFAULT, DATA_TYPE,"
                + " NUMERIC_PRECISION, NUMERIC_SCALE, CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS"
                + " WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'SHEET' ORDER BY ORDINAL_POSITION");
                ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String column = q(name);
                if ("TODAY".equals(name)) {
                    columns.add("COALESCE(CAST(@asof AS DATE), s.TODAY) AS TODAY");
                } else if (!defaults || "ID".equals(name) || "STARTED".equals(name)) {
                    columns.add("s." + column);
                } else {
                    String value = rs.getString("COLUMN_DEFAULT");
                    columns.add("CAST(" + (value == null ? "NULL" : value) + " AS " + type(rs) + ") AS " + column);
                }
            }
        }
        return String.join(", ", columns);
    }

    private static String type(ResultSet rs) throws SQLException {
        String type = rs.getString("DATA_TYPE");
        return switch (type) {
            case "NUMERIC", "DECIMAL" -> type + "(" + rs.getInt("NUMERIC_PRECISION") + ", " + rs.getInt("NUMERIC_SCALE")
                    + ")";
            case "CHARACTER VARYING" -> "VARCHAR(" + rs.getLong("CHARACTER_MAXIMUM_LENGTH") + ")";
            default -> type;
        };
    }

    private static String q(String name) {
        return "\"" + name + "\"";
    }
}
