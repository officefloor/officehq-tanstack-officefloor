package net.officefloor.hq.app.period;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Periods by sheet, earliest first. */
@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class));

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId);
    }

    /** Lock the sheet's row until the transaction ends, so concurrent period adds are checked one after the other. */
    public void lockSheet(String sheetId) {
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", rs -> null, sheetId);
    }

    public Period insert(String sheetId, String name, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                sheetId, name, Date.valueOf(from), Date.valueOf(to));
        return new Period(sheetId, name, from, to);
    }
}
