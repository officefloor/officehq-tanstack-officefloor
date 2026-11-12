package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's status log, oldest first: a line per status it has had,
 *  {@code <STATUS> by <user> on <sheet's today> due=<due as computed then, n/a when no rate applied>}. */
@Repository
public class ClaimLogStore {

    private final JdbcTemplate jdbc;
    private final ClaimStore claims;
    private final PersonStore people;
    private final RateStore rates;
    private final SettingStore settings;
    private final SheetStore sheets;

    public ClaimLogStore(JdbcTemplate jdbc, ClaimStore claims, PersonStore people, RateStore rates,
            SettingStore settings, SheetStore sheets) {
        this.jdbc = jdbc;
        this.claims = claims;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
    }

    /** Logs the claim's current status, by the user, on the sheet's today, at its due as the screen
     *  works it out now (its settled due once settled, else its figure plus any late charge). */
    public void record(Claim claim, String user) {
        String sheetId = claim.getSheetId();
        LocalDate today = sheets.today(sheetId);
        Person person = people.find(sheetId, claim.getPersonId());
        BigDecimal pct = person == null ? null
                : rates.find(sheetId, person.gradeOn(claim.getDate()), claim.getDate());
        BigDecimal due = Lateness.of(sheetId, settings, sheets, claims).due(claim, pct);
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, log_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, claim.getId(), claim.getStatus().name(), user, Date.valueOf(today),
                due == null ? "n/a" : due.setScale(2).toPlainString());
    }

    /** Every claim's log lines on the sheet, by claim id, oldest first. */
    public Map<Long, List<String>> lines(String sheetId) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT * FROM claim_log WHERE sheet_id = ? ORDER BY id", rs -> {
            lines.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>())
                    .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                            + rs.getObject("log_date", LocalDate.class) + " due=" + rs.getString("due"));
        }, sheetId);
        return lines;
    }

    /** Each claim's status at the end of the date, by claim id, as its log records it: the last status
     *  logged on or before the date. A claim logged only after it (added after it) is absent. */
    public Map<Long, ClaimStatus> statusesAt(String sheetId, LocalDate date) {
        Map<Long, ClaimStatus> statuses = new HashMap<>();
        jdbc.query("SELECT claim_id, status FROM claim_log WHERE sheet_id = ? AND log_date <= ? ORDER BY id", rs -> {
            statuses.put(rs.getLong("claim_id"), ClaimStatus.valueOf(rs.getString("status")));
        }, sheetId, Date.valueOf(date));
        return statuses;
    }

    /** The sheet's today on which the claim was last logged SETTLED; null when it never was. */
    public LocalDate settledOn(Claim claim) {
        List<LocalDate> dates = jdbc.query("SELECT log_date FROM claim_log WHERE sheet_id = ? AND claim_id = ? "
                + "AND status = ? ORDER BY id DESC LIMIT 1", (rs, n) -> rs.getObject("log_date", LocalDate.class),
                claim.getSheetId(), claim.getId(), ClaimStatus.SETTLED.name());
        return dates.isEmpty() ? null : dates.get(0);
    }

    /** The claim's log lines, oldest first. */
    public List<String> lines(Claim claim) {
        return lines(claim.getSheetId()).getOrDefault(claim.getId(), List.of());
    }
}
