package net.officefloor.hq.app.asof;

import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Every row a sheet holds, kept as its today moves on (sheet_snapshot), so the sheet can be shown as it stood at the
 * end of a date. The tables kept are found on boot: every table with a sheet column (the sheet table by its id) and
 * every table of rows kept per person (a person_id column); each gets a shadow table {@code <table>__ASOF} with the
 * same columns (bar the generated ones) and a snapshot_id. A table added later is kept with no change here.
 */
@Repository
public class SheetSnapshots {

    private static final String SHADOW = "__ASOF";

    /** Tables that are history themselves, never part of what a sheet shows. */
    private static final Set<String> SKIP = Set.of("FLYWAY_SCHEMA_HISTORY", "SHEET_STATE", "SHEET_SNAPSHOT");

    /** A table kept per sheet: its stored columns, how its rows of one sheet are found, and its primary key (null when it
     *  has none kept). */
    private record Kept(String table, String columns, String scope, String key) {
    }

    private final JdbcTemplate jdbc;
    private final SheetClock clock;

    /** In the order a parent comes before the rows that reference it. */
    private volatile List<Kept> kept = List.of();

    public SheetSnapshots(JdbcTemplate jdbc, SheetClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Finds the tables kept per sheet and makes their shadow tables (DDL, so once on boot, outside any change). */
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void prepare() {
        List<Kept> found = jdbc.execute((ConnectionCallback<List<Kept>>) (con) -> {
            DatabaseMetaData meta = con.getMetaData();
            Map<String, List<String>> columns = new LinkedHashMap<>();
            try (ResultSet rs = meta.getColumns(null, "PUBLIC", "%", "%")) {
                while (rs.next()) {
                    String table = rs.getString("TABLE_NAME");
                    if (SKIP.contains(table) || table.endsWith(SHADOW)) {
                        continue;
                    }
                    List<String> list = columns.computeIfAbsent(table, (k) -> new ArrayList<>());
                    if (!"YES".equals(rs.getString("IS_GENERATEDCOLUMN"))) {
                        list.add(rs.getString("COLUMN_NAME"));
                    }
                }
            }
            Map<String, Set<String>> parents = new HashMap<>();
            for (String table : columns.keySet()) {
                Set<String> mine = new HashSet<>();
                try (ResultSet rs = meta.getImportedKeys(null, "PUBLIC", table)) {
                    while (rs.next()) {
                        mine.add(rs.getString("PKTABLE_NAME"));
                    }
                }
                mine.remove(table);
                parents.put(table, mine);
            }
            List<Kept> out = new ArrayList<>();
            Set<String> placed = new HashSet<>();
            List<String> left = new ArrayList<>(columns.keySet());
            while (!left.isEmpty()) {
                String next = left.stream().filter((t) -> placed.containsAll(
                        parents.get(t).stream().filter(columns::containsKey).toList())).findFirst().orElse(left.get(0));
                left.remove(next);
                placed.add(next);
                List<String> cols = columns.get(next);
                String scope = "SHEET".equals(next) ? "ID = ?"
                        : cols.contains("SHEET") ? "SHEET = ?"
                        : cols.contains("PERSON_ID") ? "PERSON_ID IN (SELECT ID FROM PERSON WHERE SHEET = ?)" : null;
                List<String> key = new ArrayList<>();
                try (ResultSet rs = meta.getPrimaryKeys(null, "PUBLIC", next)) {
                    while (rs.next()) {
                        key.add(rs.getString("COLUMN_NAME"));
                    }
                }
                if (scope != null) {
                    out.add(new Kept(next, String.join(", ", cols), scope,
                            key.isEmpty() || !cols.containsAll(key) ? null : String.join(", ", key)));
                }
            }
            try (Statement st = con.createStatement()) {
                for (Kept k : out) {
                    st.execute("CREATE TABLE IF NOT EXISTS " + k.table() + SHADOW + " AS SELECT CAST(NULL AS BIGINT)"
                            + " AS SNAPSHOT_ID, " + k.columns() + " FROM " + k.table() + " WHERE FALSE");
                    st.execute("CREATE INDEX IF NOT EXISTS " + k.table() + SHADOW + "_SNAPSHOT ON " + k.table()
                            + SHADOW + " (SNAPSHOT_ID)");
                }
            }
            return out;
        });
        kept = List.copyOf(found);
    }

    /** Keeps every row the sheet holds now, as its today moves from one date to another. */
    public void keep(String sheet, LocalDate from, LocalDate to) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update((con) -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sheet_snapshot (sheet, from_today, to_today) VALUES (?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheet);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            return ps;
        }, key);
        long id = key.getKey().longValue();
        for (Kept k : kept) {
            jdbc.update("INSERT INTO " + k.table() + SHADOW + " (SNAPSHOT_ID, " + k.columns() + ") SELECT ?, "
                    + k.columns() + " FROM " + k.table() + " WHERE " + k.scope(), id, sheet);
        }
    }

    /**
     * Puts the sheet, inside the caller's transaction, as it stood at the end of the date: the rows kept when its today
     * first moved from on or before the date to after it; what it holds now when its today has never moved past the
     * date; nothing when its today was never on or before it. The date then stands in for its today. The caller must
     * roll back.
     */
    public void show(String sheet, LocalDate date) {
        List<Long> found = jdbc.queryForList("SELECT id FROM sheet_snapshot WHERE sheet = ? AND from_today <= ?"
                + " AND to_today > ? ORDER BY id LIMIT 1", Long.class, sheet, Date.valueOf(date), Date.valueOf(date));
        boolean now = found.isEmpty() && !clock.today(sheet).isAfter(date);
        if (!now) {
            for (int i = kept.size() - 1; i >= 0; i--) {
                Kept k = kept.get(i);
                jdbc.update("DELETE FROM " + k.table() + " WHERE " + k.scope(), sheet);
                // A row moved to another sheet since (keeping its key, as a transferred statement does) is put back
                // here, so it is taken from where it is now.
                if (!found.isEmpty() && k.key() != null) {
                    jdbc.update("DELETE FROM " + k.table() + " WHERE (" + k.key() + ") IN (SELECT " + k.key() + " FROM "
                            + k.table() + SHADOW + " WHERE SNAPSHOT_ID = ?)", found.get(0));
                }
            }
            if (!found.isEmpty()) {
                for (Kept k : kept) {
                    jdbc.update("INSERT INTO " + k.table() + " (" + k.columns() + ") SELECT " + k.columns() + " FROM "
                            + k.table() + SHADOW + " WHERE SNAPSHOT_ID = ?", found.get(0));
                }
            }
        }
        clock.setToday(sheet, date);
    }
}
