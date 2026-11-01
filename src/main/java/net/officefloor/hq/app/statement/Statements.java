package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The statements taken of the people on each sheet. */
@Repository
public class Statements {

    private final JdbcTemplate jdbc;

    public Statements(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Per person on the sheet with a statement: the count, and the last one's due-total and date. */
    public List<StatementSummary> list(String sheet) {
        return jdbc.query("""
                SELECT s.person, c.taken, s.due_total, s.taken_on FROM statement s
                JOIN (SELECT person, COUNT(*) AS taken, MAX(id) AS last FROM statement WHERE sheet = ? GROUP BY person) c
                  ON s.id = c.last
                ORDER BY s.person""", (rs, i) -> {
            BigDecimal due = rs.getBigDecimal("due_total");
            return new StatementSummary(rs.getLong("person"), rs.getInt("taken"),
                    due == null ? "n/a" : due.setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                    rs.getDate("taken_on").toLocalDate().toString());
        }, sheet);
    }

    /** Records a statement of the person: their due-total then (null when n/a), taken on the date. */
    public void add(String sheet, long person, BigDecimal dueTotal, LocalDate takenOn) {
        jdbc.update("INSERT INTO statement (sheet, person, due_total, taken_on) VALUES (?, ?, ?, ?)", sheet, person,
                dueTotal, Date.valueOf(takenOn));
    }
}
