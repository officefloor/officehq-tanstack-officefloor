package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each sheet's last rollover, kept so it can be undone: what it generated and the opening balances it set. */
@Repository
public class RolloverRepository {

    /** A sheet's last rollover: the fiscal year (FY<yyyy>), who made it, on which sheet-today, and the names of
     *  the periods it generated. */
    public record Last(String fy, String by, LocalDate on, List<String> periods) {
    }

    /** An opening balance a rollover set: the amount added, and whether the opening balance was new. */
    public record Opening(String period, long personId, BigDecimal amount, boolean created) {
    }

    private final JdbcTemplate jdbc;

    public RolloverRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep the rollover as the sheet's last, replacing the one before. */
    public void record(String sheetId, String fy, String by, LocalDate on, List<String> periods, List<Opening> openings) {
        forget(sheetId);
        jdbc.update("INSERT INTO sheet_rollover (sheet_id, fy, made_by, made_on, periods) VALUES (?, ?, ?, ?, ?)",
                sheetId, fy, by, on, String.join(",", periods));
        for (Opening o : openings) {
            jdbc.update("INSERT INTO sheet_rollover_opening (sheet_id, period, person_id, amount, created) "
                    + "VALUES (?, ?, ?, ?, ?)", sheetId, o.period(), o.personId(), o.amount(), o.created());
        }
    }

    /** The sheet's last rollover; null when there is none (or it was undone). */
    public Last last(String sheetId) {
        return jdbc.query("SELECT fy, made_by, made_on, periods FROM sheet_rollover WHERE sheet_id = ?",
                (rs, n) -> new Last(rs.getString("fy"), rs.getString("made_by"),
                        rs.getObject("made_on", LocalDate.class),
                        rs.getString("periods").isEmpty() ? List.of() : Arrays.asList(rs.getString("periods").split(","))),
                sheetId).stream().findFirst().orElse(null);
    }

    /** The opening balances the sheet's last rollover set. */
    public List<Opening> openings(String sheetId) {
        return jdbc.query("SELECT period, person_id, amount, created FROM sheet_rollover_opening WHERE sheet_id = ?",
                (rs, n) -> new Opening(rs.getString("period"), rs.getLong("person_id"), rs.getBigDecimal("amount"),
                        rs.getBoolean("created")), sheetId);
    }

    /** Forget the sheet's last rollover. */
    public void forget(String sheetId) {
        jdbc.update("DELETE FROM sheet_rollover_opening WHERE sheet_id = ?", sheetId);
        jdbc.update("DELETE FROM sheet_rollover WHERE sheet_id = ?", sheetId);
    }
}
