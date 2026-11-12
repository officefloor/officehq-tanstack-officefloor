package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a correction added to a person unless it names, by number, a SETTLED claim of theirs. */
public class CheckCorrection {
    public void check(@Val ClaimInput input, @Val Person person, ClaimStore claims) throws InvalidSheetException {
        refuseUnsettled(claims, person.getSheetId(), person.getId(), input);
    }

    static void refuseUnsettled(ClaimStore claims, String sheetId, long personId, ClaimInput input)
            throws InvalidSheetException {
        if (!input.isCorrection()) {
            return;
        }
        Claim corrected = claims.findByNumber(sheetId, personId, input.corrects());
        if (corrected == null || corrected.getStatus() != ClaimStatus.SETTLED) {
            throw new InvalidSheetException("A correction must name a SETTLED claim of the same person");
        }
    }
}
