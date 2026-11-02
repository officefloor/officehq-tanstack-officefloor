package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The last year end rollover kept per sheet, so it can be undone. */
@Repository
public class Rollovers {

    /** A kept rollover: who made it, on which sheet-today, the periods it generated and the openings it set. */
    public record Rollover(String fy, String by, LocalDate on, List<String> periods, List<Opening> openings) {
    }

    /** An opening balance a rollover added to a person's for a period. */
    public record Opening(String period, long person, BigDecimal amount) {
    }

    private final JdbcTemplate jdbc;

    public Rollovers(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keeps the rollover as the sheet's last, replacing the one kept before. */
    public void keep(String sheet, Rollover rollover) {
        forget(sheet);
        jdbc.update("INSERT INTO rollover (sheet, fy, made_by, made_on) VALUES (?, ?, ?, ?)", sheet, rollover.fy(),
                rollover.by(), Date.valueOf(rollover.on()));
        rollover.periods().forEach((p) -> jdbc.update("INSERT INTO rollover_period (sheet, period) VALUES (?, ?)",
                sheet, p));
        rollover.openings().forEach((o) -> jdbc.update(
                "INSERT INTO rollover_opening (sheet, period, person, amount) VALUES (?, ?, ?, ?)", sheet, o.period(),
                o.person(), o.amount()));
    }

    /** The sheet's last rollover, or null when there is none kept. */
    public Rollover last(String sheet) {
        List<Rollover> found = jdbc.query("SELECT fy, made_by, made_on FROM rollover WHERE sheet = ?",
                (rs, i) -> new Rollover(rs.getString("fy"), rs.getString("made_by"),
                        rs.getDate("made_on").toLocalDate(), new ArrayList<>(), new ArrayList<>()),
                sheet);
        if (found.isEmpty()) {
            return null;
        }
        Rollover rollover = found.get(0);
        rollover.periods().addAll(jdbc.queryForList(
                "SELECT period FROM rollover_period WHERE sheet = ? ORDER BY period", String.class, sheet));
        rollover.openings().addAll(jdbc.query("SELECT period, person, amount FROM rollover_opening WHERE sheet = ?",
                (rs, i) -> new Opening(rs.getString("period"), rs.getLong("person"), rs.getBigDecimal("amount")),
                sheet));
        return rollover;
    }

    /** Drops the sheet's kept rollover. */
    public void forget(String sheet) {
        jdbc.update("DELETE FROM rollover WHERE sheet = ?", sheet);
        jdbc.update("DELETE FROM rollover_period WHERE sheet = ?", sheet);
        jdbc.update("DELETE FROM rollover_opening WHERE sheet = ?", sheet);
    }
}
