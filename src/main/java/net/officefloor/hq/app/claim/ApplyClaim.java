package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ValidClaim request, @Val Claim claim, Sheets sheets, Periods periods, People people, Claims claims)
            throws InvalidClaimException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            // Only a draft can be edited; to change an open claim, void it and add a new one.
            throw new InvalidClaimException("Only a DRAFT claim can be edited");
        }
        String lockedBy = claims.lockedBy(claim);
        if (lockedBy != null && !lockedBy.equals(request.user())) {
            throw new InvalidClaimException("The claim is being edited by " + lockedBy);
        }
        LocalDate date = request.date() != null ? request.date() : sheets.today(claim.getSheetId());
        if (periods.isClosed(claim.getSheetId(), claim.getPeriodDate())) {
            throw new InvalidClaimException("No claim whose due-by is inside a closed period can be edited");
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (person != null && !person.allowsClaimOn(date)) {
            throw new InvalidClaimException("No claim can be dated after the person's retirement date");
        }
        ValidateClaim.checkCorrects(request, claim.getSheetId(), claim.getPersonId(), claims);
        ValidateClaim.checkRef(request.ref(), claim.getSheetId(), claim.getId(), claims);
        List<ClaimLine> lines = claims.lines(claim);
        if (!lines.isEmpty() && request.amount().compareTo(ClaimLine.sum(lines)) != 0) {
            throw new InvalidClaimException("A claim with lines takes its amount from its lines");
        }
        claim.setAmount(request.amount());
        claim.setRef(request.ref());
        claim.setTags(request.tags());
        claim.setCorrects(request.corrects());
        claim.setCorrectedDate(claims.correctedDate(claim.getSheetId(), claim.getPersonId(), request.corrects()));
        claim.setDate(date);
        claim.setNote(request.note());
        claim.setEditedBy(request.user());
    }
}
