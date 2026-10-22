package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuse an edit of a claim that is no longer a DRAFT: an OPEN claim is changed by voiding it and adding a new one. */
public class RefuseNonDraftClaimEdit {
    public void refuse(@Val Claim loaded) throws ClaimStatusNotAllowedException {
        if (loaded.status() != ClaimStatus.DRAFT) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim cannot be edited; only a DRAFT can. Void it and add a new one.");
        }
    }
}
