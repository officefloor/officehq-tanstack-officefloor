package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Splits an amount (above 0.00 and below the claim's) off an OPEN claim not deferred past the sheet's today: the claim
 * keeps the rest and loses any figure a statement bound it to; a new OPEN claim with the amount, the same date and note,
 * the next number, opened by the user, split from the claim's number and due by the terms-th business day after its date,
 * with the terms in force for its person on the sheet's today, the day it is created (their own, else their grade's on that day, else the sheet's), is listed as the person's newest;
 * a late entry when that due-by is in a soft-closed period. Opening the new claim takes the open permission: refused for a
 * user whose role may not open a claim on the sheet.
 */
public class ApplySplit {
    public void apply(@Val Claim claim, @Val SplitAmount amount, @Val ChangedBy by, ClaimRepository repository,
            PersonRepository people, RoleRepository roles, SettingRepository settings, SheetRepository sheets, PeriodRepository periods,
            Out<Split> split) throws BadRequestException {
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new BadRequestException("Only an OPEN claim is split; this one is " + claim.getStatus());
        }
        if (claim.isDeferredOn(sheets.today(claim.getSheetId()))) {
            throw new BadRequestException("A deferred claim cannot be split");
        }
        if (amount.value().signum() <= 0 || amount.value().compareTo(claim.getAmount()) >= 0) {
            throw new BadRequestException("The amount split off is more than 0.00 and less than the claim's amount");
        }
        String role = roles.roleOf(claim.getSheetId(), by.name());
        if (!settings.allows(claim.getSheetId(), "open", role)) {
            throw new BadRequestException("A " + role + " may not open a claim on this sheet");
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Claim into = repository.split(claim, amount.value(), by.name(), BuildClaim.terms(person, sheets.today(claim.getSheetId()), settings));
        if (into == null) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        if (periods.softClosedOn(claim.getSheetId(), into.getDueBy())) {
            repository.markLate(into);
        }
        into.setNumber(ApplyClaimStatus.number(into, repository, settings, sheets, periods));
        repository.updateStatus(into, ClaimStatus.OPEN);
        split.set(new Split(into));
    }
}
