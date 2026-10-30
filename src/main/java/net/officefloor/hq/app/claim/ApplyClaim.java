package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ValidClaim request, @Val Claim claim, Sheets sheets, Periods periods, People people)
            throws InvalidClaimException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            // Only a draft can be edited; to change an open claim, void it and add a new one.
            throw new InvalidClaimException("Only a DRAFT claim can be edited");
        }
        LocalDate date = request.date() != null ? request.date() : sheets.today(claim.getSheetId());
        if (periods.isClosed(claim.getSheetId(), claim.getDate()) || periods.isClosed(claim.getSheetId(), date)) {
            throw new InvalidClaimException("No claim dated inside a closed period can be edited");
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (person != null && !person.allowsClaimOn(date)) {
            throw new InvalidClaimException("No claim can be dated after the person's retirement date");
        }
        claim.setAmount(request.amount());
        claim.setDate(date);
        claim.setNote(request.note());
    }
}
