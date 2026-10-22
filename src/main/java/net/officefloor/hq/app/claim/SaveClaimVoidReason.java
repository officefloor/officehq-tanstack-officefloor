package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;

/** Record the reason a claim was voided with, when one was given. */
public class SaveClaimVoidReason {
    public void save(@Val ClaimStatusInput input, @Val Claim loaded, VoidReasonRepository reasons) {
        if (input.action().equals("void") && input.reason() != null) {
            reasons.voided(loaded.sheetId(), loaded.id(), input.reason());
        }
    }
}
