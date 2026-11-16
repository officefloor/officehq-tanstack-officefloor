package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored snapshots, kept per sheet, in the order written. */
@Repository
public class SnapshotRepository {

    private final JdbcTemplate jdbc;

    public SnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(String sheetId, long personId, LocalDate day, String dueTotal) {
        jdbc.update("INSERT INTO person_snapshot (sheet_id, person_id, left_on, due_total) VALUES (?, ?, ?, ?)", sheetId, personId,
                java.sql.Date.valueOf(day), dueTotal);
    }

    /** Every snapshot on the sheet, oldest first. */
    public List<Snapshot> list(String sheetId) {
        return jdbc.query("SELECT person_id, left_on, due_total FROM person_snapshot WHERE sheet_id = ? ORDER BY id",
                (rs, n) -> new Snapshot(rs.getLong("person_id"), rs.getDate("left_on").toLocalDate().toString(),
                        rs.getString("due_total")), sheetId);
    }

    /** As they stood at the end of the date: only those written while the sheet's today was on or before it. */
    public List<Snapshot> listAsOf(String sheetId, LocalDate date) {
        return jdbc.query("SELECT person_id, left_on, due_total FROM person_snapshot WHERE sheet_id = ? AND left_on <= ? ORDER BY id",
                (rs, n) -> new Snapshot(rs.getLong("person_id"), rs.getDate("left_on").toLocalDate().toString(),
                        rs.getString("due_total")), sheetId, java.sql.Date.valueOf(date));
    }
}
