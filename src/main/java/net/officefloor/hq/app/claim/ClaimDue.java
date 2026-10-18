package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** A claim's due as it would be fixed now: at the rate its person's grade has in force on the claim's date: amount × rate / 100,
 *  rounded half up when the claim is dated before the sheet's policy date (its today when none is set), half to even otherwise,
 *  and raised to the sheet's floor (none when no rate applies), plus, when it is overdue (its due-by, fixed when it was created, is
 *  before the sheet's today), the sheet's late percent of that floored due for every full 30 days it is overdue, rounded the
 *  same way. A correction (a negative amount) gets neither the floor nor the late charge. */
final class ClaimDue {
    private ClaimDue() {
    }

    static BigDecimal now(Claim claim, PersonRepository people, RateRepository rates, SettingRepository settings,
            SheetRepository sheets) {
        Person person = people.findById(claim.getPersonId()).orElseThrow();
        LocalDate today = sheets.findById(claim.getSheetId()).orElseThrow().getToday();
        RoundingMode rounding = settings.rounding(claim.getSheetId(), claim.getDate(), today);
        BigDecimal due = rates.findInForce(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate())
                .map((r) -> claim.getAmount().multiply(r.getRate())
                        .divide(BigDecimal.valueOf(100), 2, rounding))
                .orElse(null);
        // A correction's due is its amount × rate, rounded, with no floor and no late charge.
        if (!claim.isCorrection()) {
            due = settings.floored(claim.getSheetId(), due);
        }
        if (due != null && !claim.isCorrection()) {
            LocalDate dueBy = claim.getDueBy();
            long spans = dueBy.isBefore(today) ? ChronoUnit.DAYS.between(dueBy, today) / 30 : 0;
            if (spans > 0) {
                due = due.add(due.multiply(settings.latePct(claim.getSheetId())).multiply(BigDecimal.valueOf(spans))
                        .divide(BigDecimal.valueOf(100), 2, rounding));
            }
        }
        return due;
    }
}
