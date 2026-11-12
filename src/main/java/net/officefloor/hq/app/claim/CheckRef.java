package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim added with an external reference another claim on the sheet carries, compared without regard to case. */
public class CheckRef {
    public void check(@Val ClaimInput input, @Val Person person, ClaimStore claims) throws InvalidSheetException {
        refuseTaken(claims, person.getSheetId(), input, 0);
    }

    /** Refuses the claim's reference when a claim on the sheet other than {@code exceptId} carries it. */
    static void refuseTaken(ClaimStore claims, String sheetId, ClaimInput input, long exceptId)
            throws InvalidSheetException {
        if (input.ref() != null && claims.refTaken(sheetId, input.ref(), exceptId)) {
            throw new InvalidSheetException("The reference " + input.ref() + " is already used on the sheet");
        }
    }
}
