package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the body. A settle as of a date works out the claim's due as it stood at the end of that
 *  date (as the as-of view does, see {@link AsOf}) here, before the change's transaction: refused
 *  when the date is after the sheet's today or the claim did not exist then. */
public class ValidateClaimStatus {
    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") long claimId,
            @RequestBody ClaimStatusRequest request, Sheets sheets, AsOf asOf, Claims claims, People people,
            Figures figures, Out<ClaimStatusChange> body, Out<Actor> actor)
            throws InvalidSheetRequestException, ClaimStatusRefusedException {
        String action = request == null || request.action() == null ? "" : request.action().trim();
        ClaimAction parsed;
        try {
            actor.set(new Actor(request.user()));
            parsed = ClaimAction.valueOf(action.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidSheetRequestException("Not a claim action (open, settle, approve, reject, review, void, delete or restore): " + action);
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (parsed == ClaimAction.REJECT && reason.isEmpty()) {
            throw new InvalidSheetRequestException("A rejection needs a reason");
        }
        String kept = parsed == ClaimAction.REJECT || (parsed == ClaimAction.VOID && !reason.isEmpty()) ? reason : null;
        String onText = request.on() == null ? "" : request.on().trim();
        if (parsed != ClaimAction.SETTLE || onText.isEmpty()) {
            body.set(new ClaimStatusChange(parsed, request.user(), kept));
            return;
        }
        LocalDate on;
        try {
            on = LocalDate.parse(onText);
        } catch (DateTimeParseException e) {
            throw new ClaimStatusRefusedException("A claim can be settled as of a date only: " + onText);
        }
        if (on.isAfter(sheets.today(sheetId))) {
            throw new ClaimStatusRefusedException("A claim cannot be settled as of a date after the sheet's today");
        }
        // Null: the claim did not exist then; else its due then (n/a when it had none).
        Object[] then = asOf.at(sheetId, on, () -> {
            Claim claim = claims.find(sheetId, claimId);
            if (claim == null) return null;
            Person person = people.find(sheetId, claim.getPersonId());
            return new Object[] { figures.due(claim, person.gradeOn(claim.getDate())) };
        });
        if (then == null) {
            throw new ClaimStatusRefusedException("A claim cannot be settled as of a date before it existed");
        }
        body.set(new ClaimStatusChange(parsed, request.user(), kept, on, (BigDecimal) then[0]));
    }
}
