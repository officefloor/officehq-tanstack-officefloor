package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The holidays on each sheet. */
@Repository
public class Holidays {

    private final JdbcTemplate jdbc;

    public Holidays(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every holiday on the sheet (yyyy-MM-dd), earliest first. */
    public List<String> list(String sheet) {
        return jdbc.query("SELECT holiday_on FROM holiday WHERE sheet = ? ORDER BY holiday_on",
                (rs, i) -> rs.getDate("holiday_on").toLocalDate().toString(), sheet);
    }

    /** Whether the date is already on the sheet's list. */
    public boolean has(String sheet, LocalDate date) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM holiday WHERE sheet = ? AND holiday_on = ?", Integer.class,
                sheet, Date.valueOf(date));
        return found != null && found > 0;
    }

    public void add(String sheet, LocalDate date) {
        jdbc.update("INSERT INTO holiday (sheet, holiday_on) VALUES (?, ?)", sheet, Date.valueOf(date));
    }

    /** The date itself when it is not a holiday on the sheet, else the next day that is not one. */
    public LocalDate past(String sheet, LocalDate date) {
        Set<LocalDate> days = new HashSet<>(jdbc.query("SELECT holiday_on FROM holiday WHERE sheet = ? AND holiday_on >= ?",
                (rs, i) -> rs.getDate("holiday_on").toLocalDate(), sheet, Date.valueOf(date)));
        LocalDate on = date;
        while (days.contains(on)) {
            on = on.plusDays(1);
        }
        return on;
    }
}
