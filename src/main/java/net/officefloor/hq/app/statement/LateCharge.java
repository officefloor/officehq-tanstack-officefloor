package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Grades;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.DatedAmount;
import net.officefloor.hq.app.setting.SettingHistory;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The sheet's late charge as it is today: its late percent by date (a claim gains the one in force on each 30-day boundary past its due-by plus its grace; a claim is overdue past its own due-by, fixed when
 *  it was created, plus its grace) and its today (null: none, so nothing is overdue) and each claim's grace (days: the
 *  grace in force on its date of its person's grade in force on that date; 0 when none). */
public record LateCharge(DatedAmount pct, LocalDate today, Rounding rounding, Grace grace) {

    /** A claim's grace, in days. */
    @FunctionalInterface
    public interface Grace {
        int of(Claim claim);

        /** No grace for any claim. */
        Grace NONE = claim -> 0;

        /** The grace of the grade in force on a claim's date (its person's, by id, in `grades`) in force on that date,
         *  in the history. */
        static Grace of(SettingHistory history, Map<Long, Grades> grades) {
            return claim -> history.graceFor(grades.getOrDefault(claim.personId(), Grades.NONE).on(claim.date()),
                    claim.date());
        }
    }

    /** The sheet's late charge as it is now. */
    public static LateCharge of(String sheetId, SettingRepository settings, SheetRepository sheets,
            ClaimRepository claims, PersonRepository people) {
        Map<Long, Grades> grades = people.list(sheetId).stream()
                .collect(Collectors.toMap(Person::id, Person::grades, (a, b) -> a));
        return new LateCharge(settings.latePct(sheetId), sheets.today(sheetId),
                Rounding.of(sheetId, settings, sheets, claims), Grace.of(settings.history(sheetId), grades));
    }

    /** No late charge at all. */
    public static LateCharge none() {
        return new LateCharge(DatedAmount.always(BigDecimal.ZERO), null, Rounding.none(), Grace.NONE);
    }

    /** The day a claim's late charge counts from: its due-by plus its grace. */
    public LocalDate lateFrom(Claim claim) {
        return claim.dueBy().plusDays(grace.of(claim));
    }

    /** Whether a claim is overdue: it is OPEN or PENDING, not deferred on today, and its due-by plus its grace is
     *  before today. */
    public boolean overdue(Claim claim) {
        return claim.status().unsettled() && today != null && !claim.deferredOn(today)
                && lateFrom(claim).isBefore(today);
    }

    /** The charge on a claim whose figure is `figure`: for each full 30 days it is overdue, the late percent in force
     *  on that boundary date (its due-by plus its grace plus 30, 60, 90 ... days) of the figure plus the charges of the boundaries before it (compounding), each
     *  rounded to two decimals as the claim rounds (see Rounding), the charges added up (a boundary whose percent is 0.00 adds nothing); 0.00 when it is not
     *  overdue, or it is a correction (a negative amount), or its late charge was waived. */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        BigDecimal total = new BigDecimal("0.00");
        if (!overdue(claim) || claim.amount().signum() < 0 || claim.waived()) {
            return total;
        }
        LocalDate from = lateFrom(claim);
        long periods = ChronoUnit.DAYS.between(from, today) / 30;
        for (long n = 1; n <= periods; n++) {
            BigDecimal pct = this.pct.on(from.plusDays(30 * n));
            if (pct.signum() != 0) {
                total = total.add(figure.add(total).multiply(pct).divide(BigDecimal.valueOf(100), 2, rounding.of(claim)));
            }
        }
        return total;
    }
}
