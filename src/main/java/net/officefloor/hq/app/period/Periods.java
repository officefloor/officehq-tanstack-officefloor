package net.officefloor.hq.app.period;

import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The periods of each sheet. */
@Repository
public class Periods {

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every period on the sheet, by from-date. */
    public List<Period> onSheet(String sheetId) {
        return jdbc.query("SELECT sheet_id, name, from_date, to_date FROM period WHERE sheet_id = ? ORDER BY from_date",
                (rs, i) -> new Period(rs.getString("sheet_id"), rs.getString("name"),
                        rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate()),
                sheetId);
    }

    public void add(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
    }
}
