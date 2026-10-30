package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * A claim's status log: each status it takes is written as "<STATUS> by <user> on <date> due=<due>", the date
 * being the sheet's today and the due the claim's due as shown at that moment (n/a when it has none).
 */
@Service
public class ClaimLog {

    private final JdbcTemplate jdbc;
    private final Rollup rollup;
    private final Sheets sheets;

    public ClaimLog(JdbcTemplate jdbc, Rollup rollup, Sheets sheets) {
        this.jdbc = jdbc;
        this.rollup = rollup;
        this.sheets = sheets;
    }

    /** The date the claim was last settled (its latest SETTLED line), null if never. */
    public java.time.LocalDate settledOn(Claim claim) {
        List<Date> dates = jdbc.queryForList("SELECT log_date FROM claim_log WHERE sheet_id = ? AND claim_id = ? AND status = ?"
                + " ORDER BY id DESC", Date.class, claim.getSheetId(), claim.getId(), ClaimStatus.SETTLED.name());
        return dates.isEmpty() ? null : dates.get(0).toLocalDate();
    }

    /** The date the claim was last deleted (its latest DELETED line), null if never. */
    public java.time.LocalDate deletedOn(Claim claim) {
        List<Date> dates = jdbc.queryForList("SELECT log_date FROM claim_log WHERE sheet_id = ? AND claim_id = ? AND status = ?"
                + " ORDER BY id DESC", Date.class, claim.getSheetId(), claim.getId(), ClaimStatus.DELETED.name());
        return dates.isEmpty() ? null : dates.get(0).toLocalDate();
    }

    /** The status of the claim's latest log line, null if it has none. */
    public String lastStatus(Claim claim) {
        List<String> statuses = jdbc.queryForList("SELECT status FROM claim_log WHERE sheet_id = ? AND claim_id = ?"
                + " ORDER BY id DESC", String.class, claim.getSheetId(), claim.getId());
        return statuses.isEmpty() ? null : statuses.get(0);
    }

    /** Log the claim's (new) status, as changed by the user. */
    public void record(Claim claim, Person person, String user) {
        BigDecimal due = rollup.due(claim, person);
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, log_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), claim.getStatus().name(), user,
                Date.valueOf(sheets.today(claim.getSheetId())), due == null ? "n/a" : due.setScale(2).toPlainString());
    }
}
