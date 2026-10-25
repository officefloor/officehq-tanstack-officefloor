package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Objects;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * A steward's re-date of an OPEN or PENDING claim: refused for a new date after its person's retirement date or inside a
 * closed period. Its due-by stays as it was fixed and a figure a statement bound it to stays bound; its period follows
 * the new date, and while the sheet numbers by period a claim whose period changes takes the next number of its new
 * period's sequence (its old number is never used again).
 */
public class ApplyRedate {
    public void apply(@Val RedateTo to, @Val Claim claim, @Val ChangedBy by, ClaimRepository repository,
            PersonRepository people, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            PeriodRepository periods) throws BadRequestException {
        String sheetId = claim.getSheetId();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, by.name()))) {
            throw new BadRequestException("Only a steward re-dates a claim");
        }
        if (!claim.getStatus().isOpen()) {
            throw new BadRequestException("Only an OPEN or PENDING claim is re-dated; this one is " + claim.getStatus());
        }
        LocalDate oldDate = claim.getDate();
        LocalDate newDate = to.date();
        RefuseRetiredDate.refuse(people.find(sheetId, claim.getPersonId()), newDate);
        if (periods.closedOn(sheetId, newDate)) {
            throw new BadRequestException("The claim's new date is in a closed period");
        }
        if (!repository.redate(claim, newDate)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        LocalDate numberingFrom = settings.periodNumberingFrom(sheetId);
        boolean byPeriod = numberingFrom != null && !sheets.today(sheetId).isBefore(numberingFrom);
        if (byPeriod && claim.getNumber() != null && !Objects.equals(periodOf(periods, sheetId, oldDate),
                periodOf(periods, sheetId, newDate))) {
            // Taken only once the claim is surely re-dated, so a refused re-date uses up no number.
            claim.setNumber(ApplyClaimStatus.number(claim, repository, settings, sheets, periods));
            repository.updateStatus(claim, claim.getStatus());
        }
        to.redated(oldDate);
    }

    private static Long periodOf(PeriodRepository periods, String sheetId, LocalDate date) {
        Period period = periods.on(sheetId, date);
        return period == null ? null : period.id();
    }
}
