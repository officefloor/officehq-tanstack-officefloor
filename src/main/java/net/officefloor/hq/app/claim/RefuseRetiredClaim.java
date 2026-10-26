package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

public class RefuseRetiredClaim {
    // The date a claim is being edited to (a blank date is the sheet's today) must not be after its person's retirement.
    public void refuse(@Val ClaimInput input, @Val Claim claim, People people, Sheets sheets)
            throws ClaimAfterRetirementException {
        RefuseRetiredInput.check(people.find(claim.sheetId(), claim.personId()),
                input.date() != null ? input.date() : sheets.today(claim.sheetId()));
    }
}
