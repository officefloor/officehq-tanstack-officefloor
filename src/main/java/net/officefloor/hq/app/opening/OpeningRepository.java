package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.sql.Date;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
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

    /** Stores one opening amount for the person and period, made on the sheet's today. */
    public void add(String sheetId, long periodId, long personId, BigDecimal amount) {
        jdbc.update("INSERT INTO opening (sheet_id, period_id, person_id, amount, made_on) VALUES (?, ?, ?, ?, ?)",
                sheetId, periodId, personId, amount, Date.valueOf(sheets.today(sheetId)));
    }
}
