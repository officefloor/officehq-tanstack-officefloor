package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim's new date (blank: the sheet's today) that is after its person's retirement date. */
public class RefuseRetiredClaimDate {
    public void check(@Val Claim claim, @Val ClaimInput input, PersonRepository people, SheetRepository sheets)
            throws BadRequestException {
        RefuseRetiredDate.refuse(people.find(claim.getSheetId(), claim.getPersonId()),
                input.date() != null ? input.date() : sheets.today(claim.getSheetId()));
    }
}
