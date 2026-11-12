package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a correction added to a person unless it names, by number, a SETTLED claim of theirs, and
 *  unless it is no larger than what remains of that claim: its amount less the corrections already made
 *  against it (VOID or DELETED ones left out). */
public class CheckCorrection {
    public void check(@Val ClaimInput input, @Val Person person, ClaimStore claims) throws InvalidSheetException {
        refuseUnsettled(claims, person.getSheetId(), person.getId(), input);
    }

    static void refuseUnsettled(ClaimStore claims, String sheetId, long personId, ClaimInput input)
            throws InvalidSheetException {
        refuseUnsettled(claims, sheetId, personId, input, 0);
    }

    /** As above, leaving the claim {@code exceptId} (the one being edited, 0 for none) out of the corrections already made. */
    static void refuseUnsettled(ClaimStore claims, String sheetId, long personId, ClaimInput input, long exceptId)
            throws InvalidSheetException {
        if (!input.isCorrection()) {
            return;
        }
        Claim corrected = claims.findByNumber(sheetId, personId, input.corrects());
        if (corrected == null || corrected.getStatus() != ClaimStatus.SETTLED) {
            throw new InvalidSheetException("A correction must name a SETTLED claim of the same person");
        }
        BigDecimal remaining = corrected.getAmount().add(claims.correctedTotal(sheetId, personId, input.corrects(), exceptId));
        if (input.amount().negate().compareTo(remaining) > 0) {
            throw new InvalidSheetException("A correction may not exceed what remains of the claim it corrects ("
                    + remaining.setScale(2) + ")");
        }
    }
}
