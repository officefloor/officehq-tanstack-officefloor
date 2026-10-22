package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.hq.app.statement.LateCharge;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's status log: one line per status it has had, oldest first, as
 *  <STATUS> by <user> on <date> due=<due>, the date being the sheet's today when the status was given and the due
 *  the claim's due as its row showed it then (n/a when it had no figure). */
@Repository
public class ClaimLogRepository {

    private final JdbcTemplate jdbc;
    private final ClaimRepository claims;
    private final PersonRepository people;
    private final RateRepository rates;
    private final SettingRepository settings;
    private final SheetRepository sheets;

    public ClaimLogRepository(JdbcTemplate jdbc, ClaimRepository claims, PersonRepository people,
            RateRepository rates, SettingRepository settings, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.claims = claims;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
    }

    /** Log the claim's status as stored now, given by the user, with its due as its row shows it now. */
    public void record(Claim claim, String user) {
        String sheetId = claim.sheetId();
        Claim now = claims.find(sheetId, claim.id());
        var person = people.find(sheetId, now.personId());
        BigDecimal due = Dues.shown(now, person.grades(), rates, settings.floor(sheetId),
                LateCharge.of(sheetId, settings, sheets, claims, people));
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, on_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, now.id(), now.status().name(), user, sheets.today(sheetId), due);
    }

    /** The day (the sheet's today) the claim was last given the status, as its log records it; null when it never
     *  was, or was given it while the sheet had no today. */
    public LocalDate lastOn(String sheetId, long claimId, ClaimStatus status) {
        return jdbc.query("SELECT on_date FROM claim_log WHERE sheet_id = ? AND claim_id = ? AND status = ? ORDER BY id DESC LIMIT 1",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId, claimId, status.name());
    }

    /** The log lines of every claim on the sheet, by claim id, oldest first. */
    public Map<Long, List<String>> lines(String sheetId) {
        return lines(sheetId, null);
    }

    /** The log lines of every claim on the sheet as they stood at the end of the day (null: now), by claim id,
     *  oldest first: those given while the sheet's today was on or before it (or while it had none). */
    public Map<Long, List<String>> lines(String sheetId, LocalDate end) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT * FROM claim_log WHERE sheet_id = ? ORDER BY id", rs -> {
            LocalDate on = rs.getObject("on_date", LocalDate.class);
            if (end != null && on != null && on.isAfter(end)) {
                return;
            }
            BigDecimal due = rs.getBigDecimal("due");
            lines.computeIfAbsent(rs.getLong("claim_id"), k -> new ArrayList<>())
                    .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                            + (on != null ? on.toString() : "") + " due=" + (due != null ? due.toPlainString() : "n/a"));
        }, sheetId);
        return lines;
    }

    /** Each logged claim's status at the end of the day given, by claim id, as its log records it: the last status
     *  given on or before that day (one given while the sheet had no today counts as given by then); null when every
     *  status it has was given after it (it was added later). A claim with no log is not in the map. */
    public Map<Long, ClaimStatus> statusesAt(String sheetId, LocalDate end) {
        Map<Long, ClaimStatus> statuses = new HashMap<>();
        jdbc.query("SELECT * FROM claim_log WHERE sheet_id = ? ORDER BY id", rs -> {
            LocalDate on = rs.getObject("on_date", LocalDate.class);
            long claimId = rs.getLong("claim_id");
            if (on == null || !on.isAfter(end)) {
                statuses.put(claimId, ClaimStatus.valueOf(rs.getString("status")));
            } else {
                statuses.putIfAbsent(claimId, null);
            }
        }, sheetId);
        return statuses;
    }
}
