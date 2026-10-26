package net.officefloor.hq.app.history;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Repository;

/**
 * A sheet as it stood at the end of a date. As the sheet's today moves on, the sheet is snapshotted (schema hist): the
 * snapshot holds every change made while its today was the old one or earlier, and none made later. Read as of a date,
 * the latest snapshot taken as the today moved on from that date or before it to a later one is laid out as the live
 * tables (schema asof), its today standing at that date, and read by the app's own repositories.
 */
@Repository
public class SheetHistory {

    /** The tables the screen reads (a statement's figures among them), parents before children. */
    private static final List<String> TABLES = List.of("sheet", "person", "person_grade", "person_hold", "person_retirement",
            "person_patron", "person_alias", "person_snapshot", "person_watch", "statement", "claim", "claim_log", "claim_lock", "rate", "rate_version", "sheet_setting",
            "sheet_setting_from", "period", "period_person_total", "sheet_role", "sheet_delegation", "holiday", "claim_tag", "void_reason", "claim_comment", "claim_line", "claim_template",
            "pool", "pool_member", "plan", "plan_claim");

    private final JdbcTemplate jdbc;

    private final DataSource dataSource;

    public SheetHistory(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    /** Snapshot the sheet as its today moves on from {@code through} to the later {@code until}. */
    public long snapshot(String sheetId, LocalDate through, LocalDate until) {
        long id = jdbc.queryForObject("SELECT id FROM FINAL TABLE (INSERT INTO sheet_snapshot (sheet_id, through, until)"
                + " VALUES (?, ?, ?))", Long.class, sheetId, Date.valueOf(through), Date.valueOf(until));
        for (String table : TABLES) {
            String columns = columns(jdbc, table);
            jdbc.update("INSERT INTO hist." + table + " (" + columns + ", snapshot_id) SELECT " + columns + ", ? FROM "
                    + "public." + table + " WHERE " + ofSheet(table, "public"), id, sheetId);
        }
        return id;
    }

    /** Lay the period, as it now stands live (closed, its totals), over the snapshot: a period closed as the today
     *  moved on counts as closed by the end of the old today. */
    public void refreshPeriod(long snapshotId, long periodId) {
        for (String table : List.of("period_person_total", "period")) {
            String key = table.equals("period") ? "id" : "period_id";
            jdbc.update("DELETE FROM hist." + table + " WHERE snapshot_id = ? AND " + key + " = ?", snapshotId, periodId);
        }
        for (String table : List.of("period", "period_person_total")) {
            String key = table.equals("period") ? "id" : "period_id";
            String columns = columns(jdbc, table);
            jdbc.update("INSERT INTO hist." + table + " (" + columns + ", snapshot_id) SELECT " + columns + ", ? FROM "
                    + "public." + table + " WHERE " + key + " = ?", snapshotId, periodId);
        }
    }

    /** The snapshot holding the sheet as it stood at the end of the date; null when its today never moved on past it. */
    public Long snapshotAsOf(String sheetId, LocalDate on) {
        return jdbc.query("SELECT id FROM sheet_snapshot WHERE sheet_id = ? AND through <= ? AND until > ?"
                + " ORDER BY id DESC LIMIT 1", (rs, n) -> rs.getLong(1), sheetId, Date.valueOf(on), Date.valueOf(on))
                .stream().findFirst().orElse(null);
    }

    /**
     * Read the snapshot, its today standing at {@code on}: {@code reader} is given a JdbcTemplate over the snapshot laid
     * out as the live tables, for the app's repositories to read it as they read the live sheet.
     */
    public <T> T read(String sheetId, long snapshotId, LocalDate on, Function<JdbcTemplate, T> reader) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try {
            JdbcTemplate asOf = new JdbcTemplate(new SingleConnectionDataSource(con, true));
            for (String table : TABLES) {
                String columns = columns(asOf, table);
                asOf.update("INSERT INTO asof." + table + " (" + columns + ") SELECT " + columns + " FROM hist." + table
                        + " WHERE snapshot_id = ?", snapshotId);
            }
            asOf.update("UPDATE asof.sheet SET today = ? WHERE id = ?", Date.valueOf(on), sheetId);
            asOf.execute("SET SCHEMA asof");
            try {
                return reader.apply(asOf);
            } finally {
                asOf.execute("SET SCHEMA public");
                for (int i = TABLES.size() - 1; i >= 0; i--) {
                    String table = TABLES.get(i);
                    asOf.update("DELETE FROM asof." + table + " WHERE " + ofSheet(table, "asof"), sheetId);
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /**
     * Read the sheet as the signed-in user's screen shows it: live when {@code on} is null, else as it stood at the end
     * of that date, the date standing in for the sheet's today. {@code reader} is given a JdbcTemplate over the sheet
     * laid out as the live tables (schema asof), for the app's repositories to read it as they read the live sheet. A
     * date on or after the sheet's today reads every change made; a date before the earliest snapshot reads none.
     * The layout is rolled back once read (to a savepoint inside the request's transaction), so no other request sees it.
     */
    public <T> T view(String sheetId, LocalDate on, Function<JdbcTemplate, T> reader) {
        if (on == null) {
            return reader.apply(jdbc);
        }
        LocalDate today = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId);
        Long snapshot = today == null || !on.isBefore(today) ? null : snapshotAsOf(sheetId, on);
        Connection con = DataSourceUtils.getConnection(dataSource);
        try {
            boolean autoCommit = con.getAutoCommit();
            String schema = con.getSchema();
            Savepoint before = autoCommit ? null : con.setSavepoint();
            con.setAutoCommit(false);
            try {
                JdbcTemplate asOf = new JdbcTemplate(new SingleConnectionDataSource(con, true));
                for (String table : TABLES) {
                    String columns = columns(asOf, table);
                    if (snapshot != null) {
                        asOf.update("INSERT INTO asof." + table + " (" + columns + ") SELECT " + columns + " FROM hist."
                                + table + " WHERE snapshot_id = ?", snapshot);
                    } else if (today != null && (!on.isBefore(today) || table.equals("sheet"))) {
                        // On or after its today: every change counts. Before its earliest snapshot: none does.
                        asOf.update("INSERT INTO asof." + table + " (" + columns + ") SELECT " + columns + " FROM public."
                                + table + " WHERE " + ofSheet(table, "public"), sheetId);
                    }
                }
                if (snapshot == null && today != null && on.isBefore(today)) {
                    asOf.update("UPDATE asof.sheet SET archived = FALSE, last_settle_batch = 0 WHERE id = ?", sheetId);
                }
                asOf.update("UPDATE asof.sheet SET today = ? WHERE id = ?", Date.valueOf(on), sheetId);
                con.setSchema("ASOF");
                return reader.apply(asOf);
            } finally {
                con.setSchema(schema);
                if (before == null) {
                    con.rollback();
                    con.setAutoCommit(true);
                } else {
                    con.rollback(before);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read the sheet as of " + on, e);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /** The table's columns, as the live table lists them. */
    private static String columns(JdbcTemplate jdbc, String table) {
        return String.join(", ", jdbc.queryForList("SELECT column_name FROM information_schema.columns"
                + " WHERE table_schema = 'PUBLIC' AND table_name = ? ORDER BY ordinal_position", String.class,
                table.toUpperCase()));
    }

    /** The condition picking the sheet's rows of the table in the schema (the sheet id its one parameter). */
    private static String ofSheet(String table, String schema) {
        return switch (table) {
            case "sheet" -> "id = ?";
            case "person_grade", "person_hold", "person_retirement", "person_patron", "person_watch" ->
                "person_id IN (SELECT id FROM " + schema + ".person WHERE sheet_id = ?)";
            case "period_person_total" -> "period_id IN (SELECT id FROM " + schema + ".period WHERE sheet_id = ?)";
            default -> "sheet_id = ?";
        };
    }
}
