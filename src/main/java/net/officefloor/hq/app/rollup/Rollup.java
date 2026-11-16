package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDue;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/**
 * The sheet's figures as the screen shows them: each person's claimed (their claims that count added up; a held person's OPEN claims do not) and
 * due-total (the figures of those claims added up; n/a when their grade has no rate today and no counting claim
 * keeps a figure), and the sheet's claimed-total and due-total (everyone's added up, skipping n/a).
 *
 * A change takes the figures before it ({@link #take}) and, after its own audit lines, writes ROLLUP for every
 * person whose figures moved (a person new to the sheet counts as moved), then TOTALS when the sheet's totals
 * moved ({@link #record}).
 */
@Service
public class Rollup {

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SheetRepository sheets;
    private final SettingRepository settings;
    private final Audit audit;

    public Rollup(PersonRepository people, ClaimRepository claims, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.settings = settings;
        this.audit = audit;
    }

    /** Every person's figures on the sheet now, in the order they were added. */
    public RollupBefore take(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        BigDecimal floor = settings.floor(sheetId);
        List<Claim> all = claims.list(sheetId);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            boolean kept = false;
            for (Claim c : all) {
                if (c.personId() != person.id() || !ClaimDue.countsLive(c, person.held())) {
                    continue;
                }
                claimed = claimed.add(c.amount());
                kept |= ClaimDue.kept(c);
                BigDecimal figure = ClaimDue.figure(c, person.grade(), rates, floor);
                if (figure != null) {
                    due = due.add(figure);
                }
            }
            String dueTotal = rates.find(sheetId, person.grade(), today) == null && !kept ? "n/a"
                    : due.toPlainString();
            figures.put(person.id(), new Figures(person.name(), claimed.toPlainString(), dueTotal));
        }
        return new RollupBefore(figures);
    }

    /** Writes the ROLLUP and TOTALS lines for the figures the change moved since {@code before}. */
    public void record(String sheetId, String by, RollupBefore before) {
        Map<Long, Figures> after = take(sheetId).figures();
        for (Map.Entry<Long, Figures> e : after.entrySet()) {
            Figures was = before.figures().get(e.getKey());
            Figures now = e.getValue();
            if (was == null || !was.claimed().equals(now.claimed()) || !was.dueTotal().equals(now.dueTotal())) {
                audit.record("ROLLUP sheet=" + sheetId + " by=" + by + " person=" + now.name() + " claimed="
                        + now.claimed() + " due-total=" + now.dueTotal());
            }
        }
        String claimedBefore = claimedTotal(before.figures());
        String dueBefore = dueTotal(before.figures());
        String claimedAfter = claimedTotal(after);
        String dueAfter = dueTotal(after);
        if (!Objects.equals(claimedBefore, claimedAfter) || !Objects.equals(dueBefore, dueAfter)) {
            audit.record("TOTALS sheet=" + sheetId + " by=" + by + " claimed-total=" + claimedAfter + " due-total="
                    + dueAfter);
        }
    }

    private static String claimedTotal(Map<Long, Figures> figures) {
        return figures.values().stream().map((f) -> new BigDecimal(f.claimed()))
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add).toPlainString();
    }

    private static String dueTotal(Map<Long, Figures> figures) {
        return figures.values().stream().filter((f) -> !f.dueTotal().equals("n/a"))
                .map((f) -> new BigDecimal(f.dueTotal())).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add)
                .toPlainString();
    }
}
