package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.hq.app.statement.LateCharge;
import org.springframework.stereotype.Service;

/** Takes the sheet's figures, and records in the audit file how they moved over a change: a ROLLUP line per
 *  person whose figures changed (a person new to the sheet counting as changed), in the order they were added,
 *  then a TOTALS line when the sheet's totals changed. A change that moves no figure records nothing. */
@Service
public class Rollups {

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SheetRepository sheets;
    private final SettingRepository settings;
    private final Audit audit;

    public Rollups(PersonRepository people, ClaimRepository claims, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, Audit audit) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.settings = settings;
        this.audit = audit;
    }

    /** The sheet's figures as they are now. */
    public Rollup take(String sheetId) {
        var today = sheets.today(sheetId);
        BigDecimal floor = settings.floor(sheetId);
        LateCharge late = LateCharge.of(sheetId, settings, sheets, claims);
        List<Claim> all = claims.list(sheetId);
        Map<Long, Rollup.Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = new BigDecimal("0.00");
        BigDecimal dueTotal = new BigDecimal("0.00");
        for (Person p : people.list(sheetId)) {
            List<Claim> theirs = all.stream().filter(c -> c.personId() == p.id()).toList();
            BigDecimal claimed = theirs.stream().filter(c -> Dues.counts(c, p.holds())).map(Claim::amount)
                    .reduce(new BigDecimal("0.00"), BigDecimal::add);
            BigDecimal due = Dues.total(sheetId, theirs, p.grades(), p.holds(), today, rates, floor, late);
            claimedTotal = claimedTotal.add(claimed);
            if (due != null) {
                dueTotal = dueTotal.add(due);
            }
            figures.put(p.id(), new Rollup.Figures(p.name(), shown(claimed), due != null ? shown(due) : "n/a"));
        }
        return new Rollup(figures, shown(claimedTotal), shown(dueTotal));
    }

    /** Record how the sheet's figures moved since `before`, as a change made by `user`. */
    public void record(String sheetId, String user, Rollup before) {
        Rollup after = take(sheetId);
        for (var e : after.people().entrySet()) {
            Rollup.Figures was = before.people().get(e.getKey());
            Rollup.Figures now = e.getValue();
            if (was == null || !Objects.equals(was.claimed(), now.claimed())
                    || !Objects.equals(was.dueTotal(), now.dueTotal())) {
                audit.record("ROLLUP sheet=" + sheetId + " by=" + user + " person=" + now.name()
                        + " claimed=" + now.claimed() + " due-total=" + now.dueTotal());
            }
        }
        if (!before.claimedTotal().equals(after.claimedTotal()) || !before.dueTotal().equals(after.dueTotal())) {
            audit.record("TOTALS sheet=" + sheetId + " by=" + user + " claimed-total=" + after.claimedTotal()
                    + " due-total=" + after.dueTotal());
        }
    }

    private static String shown(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }
}
