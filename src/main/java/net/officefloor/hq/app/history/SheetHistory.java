package net.officefloor.hq.app.history;

import java.sql.Connection;
import java.sql.Date;
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

    /** The tables a statement's figures read, parents before children. */
    private static final List<String> TABLES = List.of("sheet", "person", "person_grade", "person_hold",
            "person_patron", "statement", "claim", "rate", "sheet_setting", "sheet_setting_from");

    private final JdbcTemplate jdbc;

    private final DataSource dataSource;

    public SheetHistory(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    /** Snapshot the sheet as its today moves on from {@code through} to the later {@code until}. */
    public void snapshot(String sheetId, LocalDate through, LocalDate until) {
        long id = jdbc.queryForObject("SELECT id FROM FINAL TABLE (INSERT INTO sheet_snapshot (sheet_id, through, until)"
                + " VALUES (?, ?, ?))", Long.class, sheetId, Date.valueOf(through), Date.valueOf(until));
        for (String table : TABLES) {
            String columns = columns(jdbc, table);
            jdbc.update("INSERT INTO hist." + table + " (" + columns + ", snapshot_id) SELECT " + columns + ", ? FROM "
                    + "public." + table + " WHERE " + ofSheet(table, "public"), id, sheetId);
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
            case "person_grade", "person_hold", "person_patron" ->
                "person_id IN (SELECT id FROM " + schema + ".person WHERE sheet_id = ?)";
            default -> "sheet_id = ?";
        };
    }
}
