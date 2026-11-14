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
                LocalDate.parse(claim.getDate()));
        Dues.Late late = settings.late(sheetId, today);
        String due = Dues.shown(claim, rate == null ? null : Dues.of(claim.getAmount(), rate.pct(),
                settings.floor(sheetId), late.rounding(claim.getRoundDate())), late);
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

    /** Give each claim its log lines. */
    public List<Claim> withLogs(String sheetId, List<Claim> claims) {
        Map<Long, List<String>> lines = linesOf(sheetId);
        for (Claim c : claims) c.setLog(lines.getOrDefault(c.getId(), List.of()));
        return claims;
    }
}
