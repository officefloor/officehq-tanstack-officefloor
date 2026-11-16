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
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/**
 * The sheet's figures as the screen shows them: each person's claimed (their claims that count added up; a person's OPEN or PENDING claims dated within a hold of theirs, or any once their retirement date is reached, not bound by a statement, do not, nor do deferred claims) and
 * due-total (the figures of those claims, each with its late charge, added up; n/a when their grade has no rate today and no counting claim
 * has a figure, kept or at the grade in force on its date; at most the sheet's cap), overdue-total (the figures, late charge included, of those claims that are overdue — open, their due-by plus
 * their grace (see LateCharge#lateFrom) before today — added up, skipping n/a; not capped), and the sheet's claimed-total, due-total and overdue-total (everyone's added up, skipping n/a; the due-total and
 * overdue-total leave out the single highest and the single lowest figure when three or more people have one).
 *
 * A change takes the figures before it ({@link #take}) and, after its own audit lines, writes ROLLUP for every
 * person whose figures (claimed, due-total or overdue-total) moved (a person new to the sheet counts as moved), then
 * TOTALS when any of the sheet's three totals moved ({@link #record}).
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
        BigDecimal cap = settings.cap(sheetId);
        LateCharge late = LateCharge.of(sheetId, sheets, settings, claims);
        List<Claim> all = claims.list(sheetId);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            BigDecimal overdue = BigDecimal.ZERO.setScale(2);
            boolean figured = false;
            for (Claim c : all) {
                if (c.personId() != person.id() || !ClaimDue.countsLive(c, person, today)) {
                    continue;
                }
                claimed = claimed.add(c.amount());
                BigDecimal figure = late.add(c, ClaimDue.figure(c, person.gradeOn(c.date()), rates, late.rounding()), settings.floor(sheetId, c.date()));
                figured |= figure != null;
                if (figure != null) {
                    due = due.add(figure);
                    if (ClaimDue.isOpen(c) && late.lateFrom(c).isBefore(today)) {
                        overdue = overdue.add(figure);
                    }
                }
            }
            String dueTotal = rates.find(sheetId, person.gradeOn(today), today) == null && !figured ? "n/a"
                    : SettingRepository.capped(due, cap).toPlainString();
            figures.put(person.id(), new Figures(person.name(), claimed.toPlainString(), dueTotal,
                    overdue.toPlainString()));
        }
        return new RollupBefore(figures);
    }

    /** Writes the ROLLUP and TOTALS lines for the figures the change moved since {@code before}. */
    public void record(String sheetId, String by, RollupBefore before) {
        Map<Long, Figures> after = take(sheetId).figures();
        for (Map.Entry<Long, Figures> e : after.entrySet()) {
            Figures was = before.figures().get(e.getKey());
            Figures now = e.getValue();
            if (was == null || !was.claimed().equals(now.claimed()) || !was.dueTotal().equals(now.dueTotal())
                    || !was.overdueTotal().equals(now.overdueTotal())) {
                audit.record("ROLLUP sheet=" + sheetId + " by=" + by + " person=" + now.name() + " claimed="
                        + now.claimed() + " due-total=" + now.dueTotal() + " overdue-total=" + now.overdueTotal());
            }
        }
        String claimedBefore = claimedTotal(before.figures());
        String dueBefore = dueTotal(before.figures());
        String claimedAfter = claimedTotal(after);
        String dueAfter = dueTotal(after);
        String overdueBefore = overdueTotal(before.figures());
        String overdueAfter = overdueTotal(after);
        if (!Objects.equals(claimedBefore, claimedAfter) || !Objects.equals(dueBefore, dueAfter)
                || !Objects.equals(overdueBefore, overdueAfter)) {
            audit.record("TOTALS sheet=" + sheetId + " by=" + by + " claimed-total=" + claimedAfter + " due-total="
                    + dueAfter + " overdue-total=" + overdueAfter);
        }
    }

    private static String claimedTotal(Map<Long, Figures> figures) {
        return figures.values().stream().map((f) -> new BigDecimal(f.claimed()))
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add).toPlainString();
    }

    private static String overdueTotal(Map<Long, Figures> figures) {
        return trimmedTotal(figures.values().stream().map(Figures::overdueTotal).toList());
    }

    private static String dueTotal(Map<Long, Figures> figures) {
        return trimmedTotal(figures.values().stream().map(Figures::dueTotal).toList());
    }

    /** The figures added up, skipping n/a, leaving out the single highest and the single lowest when three or more are left. */
    private static String trimmedTotal(List<String> values) {
        List<BigDecimal> figures = values.stream().filter((v) -> !v.equals("n/a")).map(BigDecimal::new).sorted().toList();
        if (figures.size() >= 3) {
            figures = figures.subList(1, figures.size() - 1);
        }
        return figures.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add).toPlainString();
    }
}
