package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The statements taken of the people on each sheet. */
@Repository
public class Statements {

    private final JdbcTemplate jdbc;

    public Statements(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Per person on the sheet with a statement: the count, and the last one's due-total and date (the date it was taken as of, else the sheet's today then). */
    public List<StatementSummary> list(String sheet) {
        return jdbc.query("""
                SELECT s.person, c.taken, s.due_total, COALESCE(s.as_of, s.taken_on) AS stated_on FROM statement s
                JOIN (SELECT person, COUNT(*) AS taken, MAX(id) AS last FROM statement WHERE sheet = ? GROUP BY person) c
                  ON s.id = c.last
                ORDER BY s.person""", (rs, i) -> {
            BigDecimal due = rs.getBigDecimal("due_total");
            return new StatementSummary(rs.getLong("person"), rs.getInt("taken"),
                    due == null ? "n/a" : due.setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                    rs.getDate("stated_on").toLocalDate().toString());
        }, sheet);
    }

    /**
     * Records a statement of the person: their due-total (null when n/a), taken on the date, as of a date (null when
     * taken now).
     */
    public void add(String sheet, long person, BigDecimal dueTotal, LocalDate takenOn, LocalDate asOf) {
        jdbc.update("INSERT INTO statement (sheet, person, due_total, taken_on, as_of) VALUES (?, ?, ?, ?, ?)", sheet,
                person, dueTotal, Date.valueOf(takenOn), asOf == null ? null : Date.valueOf(asOf));
    }

    /** The person's last statement, if they have one. */
    public Optional<Taken> last(String sheet, long person) {
        return jdbc.query("SELECT id, due_total FROM statement WHERE sheet = ? AND person = ? ORDER BY id DESC LIMIT 1",
                (rs, i) -> new Taken(rs.getLong("id"), rs.getBigDecimal("due_total")), sheet, person)
                .stream().findFirst();
    }

    /** Removes the statement. */
    public void delete(String sheet, long id) {
        jdbc.update("DELETE FROM statement WHERE sheet = ? AND id = ?", sheet, id);
    }

    /** A statement taken: its id and due-total (null when n/a). */
    public record Taken(long id, BigDecimal dueTotal) {
    }
}
