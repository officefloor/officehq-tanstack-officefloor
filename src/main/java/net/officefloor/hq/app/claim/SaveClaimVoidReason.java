package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Stores the reason a void was given with (none: nothing stored); other status changes store nothing. */
public class SaveClaimVoidReason {
    public void save(@Val ClaimAction action, @Val Claim claim, @Val ClaimActionRequest request,
            ClaimRepository repository) {
        if (action == ClaimAction.VOID && !request.reason().isEmpty()) {
            repository.saveVoidReason(claim, request.reason());
        }
    }
}
