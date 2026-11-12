package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;

/** What a sheet's late charge follows: each claim's due-by (fixed when it was added), the sheet's late
 *  percent in force on each date and its today; and the sheet's floor in force on each claim's date. An overdue claim (OPEN or PENDING, not deferred, due-by before today) gains, for every full 30 days it is
 *  overdue, the late percent in force on that boundary (its due-by plus 30, 60, 90 ... days) of its figure plus the
 *  charges of the boundaries before it (compounding), each rounded as the sheet's {@link Rounding} has the claim, the charges added up (a boundary at 0.00 adds nothing). The charge is never bound by a
 *  statement; settling freezes it into the settled due. The floor is applied last: a live due is at least the floor
 *  in force on the claim's date (none for a correction). A claim whose person's grade in force on its date has grace
 *  days in force on that date is overdue only when its due-by plus the grace is before today, and its 30-day
 *  boundaries count from its due-by plus the grace ({@link #graceBy}). */
public record Lateness(Function<LocalDate, BigDecimal> latePcts, Function<LocalDate, BigDecimal> floors,
        LocalDate today, Rounding rounding, Function<Claim, LocalDate> graceBy) {

    /** The days in each step of the late charge. */
    private static final int PERIOD_DAYS = 30;

    public static Lateness of(String sheetId, SettingStore settings, SheetStore sheets, ClaimStore claims,
            PersonStore people) {
        return new Lateness(settings.latePcts(sheetId), settings.floors(sheetId), sheets.today(sheetId),
                Rounding.of(sheetId, settings, sheets, claims),
                graced(settings.graces(sheetId), id -> people.find(sheetId, id)));
    }

    /** Each claim's due-by plus the grace days in force on its date for its person's grade then (its due-by
     *  when none), given the grace days by grade and date and each person by id. */
    public static Function<Claim, LocalDate> graced(BiFunction<String, LocalDate, Integer> graces,
            Function<Long, Person> people) {
        return claim -> {
            Person person = people.apply(claim.getPersonId());
            Integer grace = person == null || claim.getDueBy() == null ? null
                    : graces.apply(person.gradeOn(claim.getDate()), claim.getDate());
            return grace == null ? claim.getDueBy() : claim.getDueBy().plusDays(grace);
        };
    }

    /** Whether the claim is overdue: OPEN (or PENDING), not deferred, with its due-by plus any grace before today. */
    public boolean overdue(Claim claim) {
        return claim.getStatus().isOpen() && today != null && !claim.isDeferredOn(today) && claim.getDueBy() != null
                && graceBy.apply(claim).isBefore(today);
    }

    /** The late charge on a claim whose figure (bound, else the rate figure, never floored) is {@code figure}; none on
     *  a correction, nor on a claim whose charge was waived. */
    public BigDecimal charge(Claim claim, BigDecimal figure) {
        if (figure == null || !claim.getStatus().isOpen() || today == null || claim.isCorrection()
                || claim.isDeferredOn(today) || claim.isWaived()) {
            return BigDecimal.ZERO.setScale(2);
        }
        LocalDate from = graceBy.apply(claim);
        long overdue = ChronoUnit.DAYS.between(from, today);
        long periods = overdue <= 0 ? 0 : overdue / PERIOD_DAYS;
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (long n = 1; n <= periods; n++) {
            BigDecimal pct = latePcts.apply(from.plusDays(n * PERIOD_DAYS));
            total = total.add(ClaimFigure.of(figure.add(total), pct, rounding.mode(claim)));
        }
        return total;
    }

    /** The floor in force on the claim's date. */
    public BigDecimal floor(Claim claim) {
        return floors.apply(claim.getDate());
    }

    /** The claim's due now: its settled due, else the greater of its floor and its figure plus the late
     *  charge (a correction has no floor and no charge). */
    public BigDecimal due(Claim claim, BigDecimal pct) {
        return dueOf(claim, ClaimFigure.of(claim, pct, rounding));
    }

    /** The claim's due now, given its figure (see {@link ClaimFigure}). */
    public BigDecimal dueOf(Claim claim, BigDecimal figure) {
        if (figure == null || claim.getSettledDue() != null) {
            return figure;
        }
        BigDecimal charged = figure.add(charge(claim, figure));
        return claim.isCorrection() ? charged : ClaimFigure.floored(charged, floor(claim));
    }
}
