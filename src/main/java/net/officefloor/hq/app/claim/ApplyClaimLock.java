package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.ConflictException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** Takes the claim's lock for the user opening its editor; refused while another user holds it. */
public class ApplyClaimLock {
    public void apply(@Val Claim claim, @Val ChangedBy by, ClaimLockRepository locks) throws ConflictException {
        String holder = locks.lock(claim, by.name());
        if (!holder.equals(by.name())) {
            throw new ConflictException(locked(holder));
        }
    }

    static String locked(String holder) {
        return "The claim is being edited by " + holder + "; try again once they save or cancel";
    }
}
