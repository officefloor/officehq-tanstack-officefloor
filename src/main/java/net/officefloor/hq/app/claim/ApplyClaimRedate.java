package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Objects;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** A steward re-dates an OPEN or PENDING claim. Its due-by stays as it was fixed and a figure a
 *  statement bound stays bound; its period follows the new date, and while per-period numbering is
 *  on, a claim whose period changes takes the next number of its new period's sequence (its old
 *  number is never given again). Refused: anyone but a steward, a claim not OPEN or PENDING, a claim
 *  dated in a closed period or a new date in one, or a new date after its person's retirement. */
public class ApplyClaimRedate {
    public void apply(@Val ClaimRedate change, @Val Claim claim, People people, Settings settings, Sheets sheets,
            Periods periods, ClosedPeriods closedPeriods, ClaimNumbers numbers)
            throws ClaimStatusRefusedException, PeriodClosedException, PersonRetiredException {
        String sheetId = claim.getSheetId();
        if (sheets.archived(sheetId) || settings.role(sheetId, change.user()) != Role.STEWARD) {
            throw new ClaimStatusRefusedException("Only a steward can re-date a claim");
        }
        if (!claim.getStatus().isOpen()) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim can be re-dated");
        }
        closedPeriods.refuse(sheetId, claim.getDate());
        closedPeriods.refuse(sheetId, change.date());
        people.find(sheetId, claim.getPersonId()).refuseRetired(change.date());
        LocalDate old = claim.getDate();
        change.redated(old);
        claim.setDate(change.date());
        LocalDate from = settings.periodNumberingFrom(sheetId);
        boolean perPeriod = from != null && !sheets.today(sheetId).isBefore(from);
        if (perPeriod && claim.getNumber() != null
                && !Objects.equals(sequence(periods, sheetId, old), sequence(periods, sheetId, change.date()))) {
            claim.setNumber(numbers.next(claim));
        }
    }

    private static String sequence(Periods periods, String sheetId, LocalDate date) {
        return periods.onSheet(sheetId).stream().filter(p -> p.contains(date))
                .map(Period::name).findFirst().orElse(ClaimNumbers.NO_PERIOD);
    }
}
