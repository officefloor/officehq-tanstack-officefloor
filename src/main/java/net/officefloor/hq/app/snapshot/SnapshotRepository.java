package net.officefloor.hq.app.snapshot;

import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SnapshotRepository {

    private final JdbcTemplate jdbc;

    public SnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people's snapshots, by person and then oldest first. */
    public List<Snapshot> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, person_id, left_day, due_total FROM person_snapshot WHERE sheet_id = ?"
                + " ORDER BY person_id, id",
                (rs, n) -> new Snapshot(rs.getString("sheet_id"), rs.getLong("person_id"),
                        rs.getDate("left_day").toLocalDate(), rs.getString("due_total")),
                sheetId);
    }

    public void save(Snapshot snapshot) {
        jdbc.update("INSERT INTO person_snapshot (sheet_id, person_id, left_day, due_total) VALUES (?, ?, ?, ?)",
                snapshot.sheetId(), snapshot.personId(), Date.valueOf(snapshot.day()), snapshot.dueTotal());
    }
}
