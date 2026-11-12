package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Cancels the person's last statement: removes it and unbinds each of their claims, so they follow their
 *  live figures again. A figure bound by the statement before it is not restored. Refused when the person
 *  has no statement. */
public class CancelStatement {
    public void cancel(@Val Person person, ClaimStore claims, StatementStore statements,
            Out<Statement> cancelledOut) throws InvalidSheetException {
        String sheetId = person.getSheetId();
        // Locked, so a statement or status change made at once waits for this one.
        var bound = claims.listForUpdate(sheetId, person.getId());
        Statement last = statements.removeLast(sheetId, person.getId());
        if (last == null) {
            throw new InvalidSheetException("No statement to cancel");
        }
        for (Claim claim : bound) {
            if (claim.isStated() || claim.getStatedDue() != null || claim.isStatedNa()) {
                claim.setStated(false);
                claim.setStatedDue(null);
                claim.setStatedNa(false);
                claims.bind(claim);
            }
        }
        cancelledOut.set(last);
    }
}
