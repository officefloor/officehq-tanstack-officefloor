package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The people's snapshot lines, one per SNAPSHOT written for them (read with the person, see PersonRepository). */
@Repository
public class PersonSnapshotRepository {

    private final JdbcTemplate jdbc;

    public PersonSnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep the person's due-total (null: n/a) as it stood on the day being left. */
    public void insert(String sheetId, long personId, LocalDate day, BigDecimal dueTotal) {
        jdbc.update("INSERT INTO person_snapshot (sheet_id, person_id, left_on, due_total) VALUES (?, ?, ?, ?)", sheetId,
                personId, Date.valueOf(day), dueTotal);
    }
}
