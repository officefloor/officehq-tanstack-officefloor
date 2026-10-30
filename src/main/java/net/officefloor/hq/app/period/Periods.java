package net.officefloor.hq.app.period;

import java.sql.Date;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The period store: named, non-overlapping date ranges (both ends inclusive), kept per sheet. */
@Repository
public class Periods {

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<PeriodResponse> list(String sheetId) {
        return jdbc.query("SELECT name, from_date, to_date FROM period WHERE sheet_id = ? ORDER BY from_date",
                (rs, i) -> new PeriodResponse(rs.getString("name"), rs.getDate("from_date").toLocalDate().toString(),
                        rs.getDate("to_date").toLocalDate().toString()),
                sheetId);
    }

    /** Add a period, refusing a name already on the sheet or dates overlapping another period. */
    public void add(String sheetId, ValidPeriod period) throws InvalidPeriodException {
        // Lock the sheet's row so two periods added at once cannot both pass the overlap check.
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", (rs, i) -> rs.getString(1), sheetId);
        Integer named = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, period.name());
        if (named != null && named > 0) {
            throw new InvalidPeriodException("A period named " + period.name() + " is already on the sheet");
        }
        Integer overlapping = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(period.to()), Date.valueOf(period.from()));
        if (overlapping != null && overlapping > 0) {
            throw new InvalidPeriodException("The period's dates overlap another period");
        }
        try {
            jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                    sheetId, period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
        } catch (DuplicateKeyException raced) {
            throw new InvalidPeriodException("A period named " + period.name() + " is already on the sheet");
        }
    }
}
