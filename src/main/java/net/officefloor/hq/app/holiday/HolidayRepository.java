package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored holidays, kept per sheet. */
@Repository
public class HolidayRepository {

    private final JdbcTemplate jdbc;

    public HolidayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, by date. */
    public List<LocalDate> list(String sheetId) {
        return jdbc.query("SELECT holiday_date FROM holiday WHERE sheet_id = ? ORDER BY holiday_date",
                (rs, n) -> rs.getObject("holiday_date", LocalDate.class), sheetId);
    }

    /** Adds the holiday; false when the date is already one (the primary key refuses a second). */
    public boolean add(String sheetId, LocalDate date) {
        try {
            jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, Date.valueOf(date));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The date itself when it is not a holiday of the sheet, else the next day that is not. */
    public LocalDate notOnHoliday(String sheetId, LocalDate date) {
        Set<LocalDate> holidays = new HashSet<>(list(sheetId));
        LocalDate day = date;
        while (holidays.contains(day)) {
            day = day.plusDays(1);
        }
        return day;
    }
}
