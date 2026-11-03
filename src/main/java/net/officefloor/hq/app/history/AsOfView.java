package net.officefloor.hq.app.history;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.server.http.HttpHeader;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** A signed-in user's screen showing a sheet as of a date (the X-As-Of header, yyyy-mm-dd): the sheet as it stood
 *  at the end of that day. Every change kept in row_change (see {@link RowCapture}) made while the sheet's today
 *  was on or before the date counts, in the order made, every later one does not (the sheet itself always
 *  exists); the date stands in for the sheet's today. A read replays those changes into the asof schema's tables,
 *  runs the usual read against them, and removes them again, all in one transaction, so no other read sees them.
 *  The sheets it falls back to (its parent as it stood then, that one's parent and so on, and
 *  the sheets it was cloned from) are not shown as of the
 *  date: their rows are read as they are now, so a rate it inherits applies as it does live.
 *  While the date is set, every change is refused. */
@Component
public class AsOfView {

    public static final String HEADER = "X-As-Of";

    private final JdbcTemplate jdbc;
    private final RowCaptureInstaller installer;
    private final TransactionTemplate transaction;

    public AsOfView(JdbcTemplate jdbc, RowCaptureInstaller installer, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.installer = installer;
        this.transaction = new TransactionTemplate(transactions);
    }

    /** The as-of date the request's screen shows; null when it shows the sheet as it is now. */
    public static LocalDate of(ServerHttpConnection connection) throws BadRequestException {
        String raw = "";
        for (HttpHeader header : connection.getRequest().getHeaders()) {
            if (HEADER.equalsIgnoreCase(header.getName())) {
                raw = header.getValue() == null ? "" : header.getValue().trim();
                break;
            }
        }
        if (raw.isEmpty()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("as-of date must be yyyy-mm-dd");
        }
    }

    /** Refuse any change made from a screen showing the sheet as of a date (400, nothing written). */
    public static void refuse(ServerHttpConnection connection) throws BadRequestException {
        if (of(connection) != null) {
            throw new BadRequestException("the sheet is shown as of a date: nothing can be changed");
        }
    }

    /** The read, as the request's screen shows the sheet: as of its date when one is set, else as it is now. */
    public <T> T read(ServerHttpConnection connection, String sheetId, Supplier<T> read) throws BadRequestException {
        LocalDate day = of(connection);
        if (day == null) return read.get();
        return readAt(sheetId, day, read);
    }

    /** The read run against the sheet as it stood at the end of a day, the day standing in for its today. */
    public <T> T readAt(String sheetId, LocalDate day, Supplier<T> read) {
        return transaction.execute(status -> {
            Map<String, List<Object[]>> rows = replay(sheetId, day);
            for (Map.Entry<String, List<Object[]>> e : rows.entrySet()) {
                RowCaptureInstaller.Table table = installer.tables().get(e.getKey());
                String marks = String.join(", ", table.columns().stream().map(c -> "?").toList());
                for (Object[] row : e.getValue()) {
                    jdbc.update("INSERT INTO ASOF.\"" + table.name() + "\" VALUES (" + marks + ")", row);
                }
            }
            if (rows.getOrDefault("SHEET", List.of()).isEmpty()) {
                jdbc.update("INSERT INTO ASOF.SHEET SELECT * FROM PUBLIC.SHEET WHERE id = ?", sheetId);
            }
            jdbc.update("UPDATE ASOF.SHEET SET today = ? WHERE id = ?", Date.valueOf(day), sheetId);
            List<String> shown = new ArrayList<>(List.of(sheetId));
            for (String at = parent("ASOF", sheetId); at != null && !shown.contains(at); at = parent("PUBLIC", at)) {
                shown.add(at);
                copy(at);
            }
            for (String at : fallbacks(sheetId)) {
                if (shown.contains(at)) continue;
                shown.add(at);
                copy(at);
            }
            jdbc.execute("SET SCHEMA ASOF");
            try {
                return read.get();
            } finally {
                jdbc.execute("SET SCHEMA PUBLIC");
                for (String at : shown) {
                    for (RowCaptureInstaller.Table table : installer.tables().values()) {
                        jdbc.update("DELETE FROM ASOF.\"" + table.name() + "\" WHERE \""
                                + table.columns().get(table.sheetColumn()) + "\" = ?", at);
                    }
                }
            }
        });
    }

    /** Copy a sheet's rows as they are now into the asof schema. */
    private void copy(String sheetId) {
        for (RowCaptureInstaller.Table table : installer.tables().values()) {
            jdbc.update("INSERT INTO ASOF.\"" + table.name() + "\" SELECT * FROM PUBLIC.\"" + table.name()
                    + "\" WHERE \"" + table.columns().get(table.sheetColumn()) + "\" = ?", sheetId);
        }
    }

    /** Every sheet a sheet may fall back to: its parent and the sheet it was cloned from as they stood then, then
     *  theirs as they are now, and so on. */
    private List<String> fallbacks(String sheetId) {
        List<String> found = new ArrayList<>();
        java.util.Deque<String> pending = new java.util.ArrayDeque<>(List.of(sheetId));
        while (!pending.isEmpty()) {
            String at = pending.poll();
            String schema = at.equals(sheetId) ? "ASOF" : "PUBLIC";
            for (Map<String, Object> row : jdbc.queryForList("SELECT parent_id, cloned_from FROM " + schema + ".SHEET WHERE id = ?",
                    at)) {
                for (Object next : row.values()) {
                    if (next != null && !next.equals(sheetId) && !found.contains(next)) {
                        found.add((String) next);
                        pending.add((String) next);
                    }
                }
            }
        }
        return found;
    }

    /** A sheet's parent in a schema; null when it has none (or is not there). */
    private String parent(String schema, String sheetId) {
        List<String> found = jdbc.queryForList("SELECT parent_id FROM " + schema + ".SHEET WHERE id = ?", String.class, sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The sheet's rows as of the end of a day, by table: the kept changes made while its today was on or before
     *  it, applied in the order made. An insert adds its row (the sheet's own always counts); an update changes
     *  the columns it changed of the row it changed (found by its primary key, else by all its values), and a
     *  delete removes that row; one whose row is not there (it was added later) changes nothing. */
    private Map<String, List<Object[]>> replay(String sheetId, LocalDate day) {
        Map<String, List<Object[]>> rows = new LinkedHashMap<>();
        Date on = Date.valueOf(day);
        jdbc.query("SELECT on_date, table_name, old_row, new_row FROM ROW_CHANGE WHERE sheet_id = ? ORDER BY id", rs -> {
            RowCaptureInstaller.Table table = installer.tables().get(rs.getString("table_name"));
            if (table == null) return;
            Object[] before = RowCapture.read(rs.getBytes("old_row"));
            Object[] after = RowCapture.read(rs.getBytes("new_row"));
            boolean counts = !rs.getDate("on_date").after(on) || (before == null && table.name().equals("SHEET"));
            if (!counts) return;
            List<Object[]> kept = rows.computeIfAbsent(table.name(), k -> new ArrayList<>());
            if (before == null) {
                if (!table.key().isEmpty()) {
                    int same = find(kept, table, after);
                    if (same >= 0) kept.remove(same);
                }
                kept.add(after.clone());
                return;
            }
            int found = find(kept, table, before);
            if (found < 0) return;
            if (after == null) {
                kept.remove(found);
                return;
            }
            Object[] row = kept.get(found);
            for (int i = 0; i < row.length && i < after.length; i++) {
                if (!Objects.deepEquals(before[i], after[i])) row[i] = after[i];
            }
        }, sheetId);
        return rows;
    }

    /** The position of the kept row a change's row is: the one with its primary key, else with all its values. */
    private static int find(List<Object[]> kept, RowCaptureInstaller.Table table, Object[] row) {
        for (int i = kept.size() - 1; i >= 0; i--) {
            Object[] k = kept.get(i);
            boolean same = true;
            if (table.key().isEmpty()) {
                same = Arrays.deepEquals(k, row);
            } else {
                for (int c : table.key()) same &= Objects.deepEquals(k[c], row[c]);
            }
            if (same) return i;
        }
        return -1;
    }
}
