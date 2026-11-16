package net.officefloor.hq.app.period;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The stored periods of each sheet. */
@Repository
public class PeriodRepository {

    private static final String COLUMNS = "id, sheet_id, name, from_date, to_date";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? ORDER BY from_date, id",
                PeriodRepository::map, sheetId);
    }

    /** Whether the name is already a period's on the sheet, case and outer spaces ignored. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND LOWER(name) = LOWER(?)",
                Integer.class, sheetId, name.trim());
        return n != null && n > 0;
    }

    /** Whether any period of the sheet shares a day with the dates from..to, both inclusive. */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(to), Date.valueOf(from));
        return n != null && n > 0;
    }

    /** Stores a new period and returns it with its id. */
    public Period add(String sheetId, String name, LocalDate from, LocalDate to) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update((con) -> {
            var ps = con.prepareStatement("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            return ps;
        }, keys);
        return new Period(keys.getKey().longValue(), sheetId, name, from, to);
    }

    private static Period map(ResultSet rs, int n) throws SQLException {
        return new Period(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate());
    }
}
