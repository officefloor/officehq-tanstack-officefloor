package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The statements taken of each person, in the order they were taken (by id). */
@Repository
public class StatementRepository {

    private final JdbcTemplate jdbc;

    public StatementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String sheetId, long personId, BigDecimal dueTotal, LocalDate takenOn) {
        jdbc.update("INSERT INTO statement (sheet_id, person_id, due_total, taken_on) VALUES (?, ?, ?, ?)", sheetId,
                personId, dueTotal, Date.valueOf(takenOn));
    }

    /** A statement taken: its id, due-total (null: n/a) and date. */
    public record Taken(long id, BigDecimal dueTotal, LocalDate takenOn) {
    }

    /** The person's last statement, or null when none was taken. */
    public Taken last(String sheetId, long personId) {
        return jdbc.query("SELECT id, due_total, taken_on FROM statement WHERE sheet_id = ? AND person_id = ?"
                + " ORDER BY id DESC LIMIT 1", (rs, n) -> new Taken(rs.getLong("id"), rs.getBigDecimal("due_total"),
                        rs.getObject("taken_on", LocalDate.class)), sheetId, personId).stream().findFirst().orElse(null);
    }

    /** Remove the statement: false when it is already gone. */
    public boolean delete(long id) {
        return jdbc.update("DELETE FROM statement WHERE id = ?", id) == 1;
    }
}
