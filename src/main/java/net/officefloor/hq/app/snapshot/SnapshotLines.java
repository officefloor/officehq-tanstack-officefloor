package net.officefloor.hq.app.snapshot;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The snapshot lines of the people on a sheet: one per SNAPSHOT written for them, oldest first. */
@Repository
public class SnapshotLines {

    private static final RowMapper<SnapshotLine> ROW = (rs, i) -> new SnapshotLine(rs.getLong("person_id"),
            rs.getDate("left_day").toLocalDate().toString(), rs.getString("due_total"));

    private final JdbcTemplate jdbc;

    public SnapshotLines(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every snapshot line on the sheet, oldest first. */
    public List<SnapshotLine> list(String sheet) {
        return jdbc.query("SELECT person_id, left_day, due_total FROM person_snapshot WHERE sheet = ? ORDER BY id", ROW,
                sheet);
    }

    public void add(String sheet, long person, LocalDate day, String dueTotal) {
        jdbc.update("INSERT INTO person_snapshot (sheet, person_id, left_day, due_total) VALUES (?, ?, ?, ?)", sheet, person,
                Date.valueOf(day), dueTotal);
    }
}
