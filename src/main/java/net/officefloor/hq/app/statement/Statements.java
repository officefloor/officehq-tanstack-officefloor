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
                SELECT s.person, (SELECT COUNT(*) FROM statement c WHERE c.sheet = s.sheet AND c.person = s.person)
                  AS taken, s.due_total, COALESCE(s.as_of, s.taken_on) AS stated_on FROM statement s
                WHERE s.sheet = ? AND NOT EXISTS (SELECT 1 FROM statement o WHERE o.sheet = s.sheet
                  AND o.person = s.person AND (o.seq > s.seq OR (o.seq = s.seq AND o.id > s.id)))
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
        jdbc.update("INSERT INTO statement (sheet, person, due_total, taken_on, as_of, seq) VALUES (?, ?, ?, ?, ?,"
                + " (SELECT COALESCE(MAX(s.seq), 0) + 1 FROM statement s WHERE s.sheet = ? AND s.person = ?))", sheet,
                person, dueTotal, Date.valueOf(takenOn), asOf == null ? null : Date.valueOf(asOf), sheet, person);
    }

    /**
     * Moves the person's statements to the person {@code into} on the sheet {@code to}, keeping their order. When
     * that person has statements of their own, the moved ones are placed before them, so their last stays theirs.
     */
    public void move(String sheet, long person, String to, long into) {
        Long shift = jdbc.queryForObject("SELECT (SELECT MIN(seq) FROM statement WHERE sheet = ? AND person = ?)"
                + " - (SELECT MAX(seq) FROM statement WHERE sheet = ? AND person = ?) - 1", Long.class, to, into, sheet,
                person);
        jdbc.update("UPDATE statement SET sheet = ?, person = ?, seq = seq + ? WHERE sheet = ? AND person = ?", to, into,
                shift == null ? 0L : shift, sheet, person);
    }

    /** The person's last statement, if they have one. */
    public Optional<Taken> last(String sheet, long person) {
        return jdbc.query("SELECT id, due_total FROM statement WHERE sheet = ? AND person = ? ORDER BY seq DESC, id DESC LIMIT 1",
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
