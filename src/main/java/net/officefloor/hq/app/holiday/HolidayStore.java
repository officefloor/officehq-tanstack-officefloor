package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The holidays per sheet. A claim's due-by, fixed when it is added, is moved past those on the sheet then
 *  (see {@link #nextWorkingDay}); a holiday added later leaves existing due-by dates as they are. */
@Repository
public class HolidayStore {

    private final JdbcTemplate jdbc;

    public HolidayStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, earliest first. */
    public List<LocalDate> list(String sheetId) {
        return jdbc.queryForList("SELECT holiday_date FROM holiday WHERE sheet_id = ? ORDER BY holiday_date",
                LocalDate.class, sheetId);
    }

    /** The sheet's holidays at the end of the date (see {@link History}), earliest first. */
    public List<LocalDate> listAsOf(String sheetId, LocalDate date) {
        return jdbc.queryForList("SELECT holiday_date FROM " + History.asOf("holiday", "id", date)
                + " WHERE sheet_id = ? ORDER BY holiday_date", LocalDate.class, sheetId);
    }

    public boolean has(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM holiday WHERE sheet_id = ? AND holiday_date = ?",
                Integer.class, sheetId, date);
        return n != null && n > 0;
    }

    public void add(String sheetId, LocalDate date) {
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, date);
    }

    /** The date itself when it is not a holiday on the sheet now, else the next day that is not. */
    public LocalDate nextWorkingDay(String sheetId, LocalDate date) {
        Set<LocalDate> holidays = new HashSet<>(list(sheetId));
        LocalDate d = date;
        while (holidays.contains(d)) {
            d = d.plusDays(1);
        }
        return d;
    }
}
