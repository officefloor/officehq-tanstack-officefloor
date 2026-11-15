package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's status log: one line per status it has had, oldest first, as
 *  {@code <STATUS> by <user> on <date> due=<due>} (the sheet's today and the due it showed then; n/a: none). */
@Repository
public class ClaimLogRepository {

    private final JdbcTemplate jdbc;
    private final PersonRepository people;
    private final RateRepository rates;
    private final SettingRepository settings;
    private final SheetRepository sheets;

    public ClaimLogRepository(JdbcTemplate jdbc, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
    }

    /** Log the claim's status now: by the user, on the sheet's today, with the due it shows now. */
    public void record(Claim claim, String by) {
        String sheetId = claim.getSheetId();
        LocalDate today = sheets.today(sheetId);
        Person person = people.find(sheetId, claim.getPersonId());
        Rate rate = person == null ? null : rates.find(sheetId, person.gradeOn(claim.getDate()),
                LocalDate.parse(claim.getDate()), claim.getId());
        Dues.Late late = settings.late(sheetId, today);
        String due = Dues.shown(claim, rate == null ? null : Dues.of(claim.getAmount(), rate.pct(),
                late.rounding(claim.getRoundDate())), settings.floor(sheetId).on(claim.getDate()), late);
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, on_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, claim.getId(), claim.getStatus(), by == null ? "" : by, today,
                due == null ? null : new BigDecimal(due));
    }

    /** The sheet's log lines by claim id, oldest first. */
    public Map<Long, List<String>> linesOf(String sheetId) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, status, by_user, on_date, due FROM claim_log WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    BigDecimal due = rs.getBigDecimal("due");
                    lines.computeIfAbsent(rs.getLong("claim_id"), (k) -> new ArrayList<>())
                            .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                                    + rs.getObject("on_date", LocalDate.class) + " due="
                                    + (due == null ? "n/a" : due.setScale(2).toPlainString()));
                }, sheetId);
        return lines;
    }

    /** The day the claim was last logged SETTLED (null: never). */
    public LocalDate settledOn(String sheetId, long claimId) {
        List<LocalDate> dates = jdbc.query("SELECT on_date FROM claim_log WHERE sheet_id = ? AND claim_id = ? "
                + "AND status = 'SETTLED' ORDER BY id DESC", (rs, i) -> rs.getObject("on_date", LocalDate.class),
                sheetId, claimId);
        return dates.isEmpty() ? null : dates.get(0);
    }

    /** The day the claim was last logged DELETED (null: never). */
    public LocalDate deletedOn(String sheetId, long claimId) {
        List<LocalDate> dates = jdbc.query("SELECT on_date FROM claim_log WHERE sheet_id = ? AND claim_id = ? "
                + "AND status = 'DELETED' ORDER BY id DESC", (rs, i) -> rs.getObject("on_date", LocalDate.class),
                sheetId, claimId);
        return dates.isEmpty() ? null : dates.get(0);
    }

    /** Each logged claim's status at the end of the day, as its log records it: the last status
     *  logged on or before it, or null when it was added after it. Claims with no log are absent. */
    public Map<Long, String> statusesOn(String sheetId, LocalDate date) {
        Map<Long, String> statuses = new HashMap<>();
        jdbc.query("SELECT claim_id, status, on_date FROM claim_log WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    long claimId = rs.getLong("claim_id");
                    if (!rs.getObject("on_date", LocalDate.class).isAfter(date)) {
                        statuses.put(claimId, rs.getString("status"));
                    } else {
                        statuses.putIfAbsent(claimId, null);
                    }
                }, sheetId);
        return statuses;
    }

    /** Give each claim its log lines. */
    public List<Claim> withLogs(String sheetId, List<Claim> claims) {
        Map<Long, List<String>> lines = linesOf(sheetId);
        for (Claim c : claims) c.setLog(lines.getOrDefault(c.getId(), List.of()));
        return claims;
    }
}
