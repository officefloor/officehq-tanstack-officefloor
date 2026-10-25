package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The periods of each sheet, none overlapping another on its sheet. */
@Repository
public class PeriodRepository {

    static final String NAME_TAKEN = "That period name is already in use on this sheet";

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getString("name"), rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate(),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"));

    private static final String COLUMNS = "id, sheet_id, name, from_date, to_date, closed, closed_total";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ?"
                + " ORDER BY from_date, id", ROW, sheetId);
    }

    /** Whether a period on the sheet has this name, ignoring case and surrounding spaces. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND name_key = LOWER(TRIM(?))",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether a period on the sheet shares a day with the dates from..to (inclusive). */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(to), Date.valueOf(from));
        return n != null && n > 0;
    }

    public Period insert(String sheetId, String name, LocalDate from, LocalDate to) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            return ps;
        }, keys);
        return new Period(keys.getKey().longValue(), sheetId, name, from, to, false, null);
    }

    public Period find(String sheetId, long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    /** Whether the date falls inside a closed period of the sheet. */
    public boolean closedOn(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND closed AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Close the period with its due-total fixed. Whether it was still open: of two users closing it, one wins. */
    public boolean close(long id, BigDecimal total) {
        return jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE id = ? AND NOT closed", total,
                id) == 1;
    }

    /** Reopen the period, its due-total live again. Whether it was still closed: of two users reopening it, one wins. */
    public boolean reopen(long id) {
        return jdbc.update("UPDATE period SET closed = FALSE, closed_total = NULL WHERE id = ? AND closed", id) == 1;
    }
}
