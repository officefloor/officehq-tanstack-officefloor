package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The periods on each sheet. */
@Repository
public class PeriodStore {

    private final JdbcTemplate jdbc;

    public PeriodStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every period on the sheet, by from-date. */
    public List<PeriodResponse> list(String sheetId) {
        return jdbc.query("SELECT name, from_date, to_date FROM period WHERE sheet_id = ? ORDER BY from_date",
                (rs, i) -> PeriodResponse.of(rs.getString("name"), rs.getObject("from_date", LocalDate.class),
                        rs.getObject("to_date", LocalDate.class)),
                sheetId);
    }

    /** Whether a period on the sheet has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND LOWER(name) = LOWER(?)",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether a period on the sheet shares a day with from..to (inclusive). */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?", Integer.class,
                sheetId, to, from);
        return n != null && n > 0;
    }

    public void add(String sheetId, PeriodInput period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)", sheetId,
                period.name(), period.from(), period.to());
    }
}
