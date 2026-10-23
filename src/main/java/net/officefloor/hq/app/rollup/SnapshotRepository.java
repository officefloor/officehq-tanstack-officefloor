package net.officefloor.hq.app.rollup;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The snapshots written for each person, per sheet, in the order written. */
@Repository
public class SnapshotRepository {

    private final JdbcTemplate jdbc;

    public SnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(String sheetId, long personId, LocalDate day, String dueTotal) {
        jdbc.update("INSERT INTO person_snapshot (sheet_id, person_id, left_on, due_total) VALUES (?, ?, ?, ?)", sheetId,
                personId, day, dueTotal);
    }

    /** Every snapshot on the sheet, oldest first. */
    public List<Snapshot> all(String sheetId) {
        return jdbc.query("SELECT person_id, left_on, due_total FROM person_snapshot WHERE sheet_id = ? ORDER BY id",
                (rs, n) -> new Snapshot(rs.getLong("person_id"), rs.getObject("left_on", LocalDate.class).toString(),
                        rs.getString("due_total")),
                sheetId);
    }
}
