package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.sql.Date;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The opening balances pasted onto each sheet: amounts added to a person's carried figure for a period (read
 *  with the period, see PeriodRepository#openings). */
@Repository
public class OpeningRepository {

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;

    public OpeningRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** Stores one opening amount for the person and period, made on the sheet's today; returns its id. */
    public long add(String sheetId, long periodId, long personId, BigDecimal amount) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update((con) -> {
            var ps = con.prepareStatement("INSERT INTO opening (sheet_id, period_id, person_id, amount, made_on) VALUES (?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, periodId);
            ps.setLong(3, personId);
            ps.setBigDecimal(4, amount);
            ps.setDate(5, Date.valueOf(sheets.today(sheetId)));
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }
}
