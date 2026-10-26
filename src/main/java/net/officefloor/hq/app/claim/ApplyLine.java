package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** Keeps the line on the claim and sets its amount to its lines' total; adding a line counts as editing the claim. */
public class ApplyLine {
    public void apply(@Val Claim claim, @Val ClaimLine line, @Val ChangedBy by, ClaimLineRepository lines,
            ClaimRepository repository) {
        lines.add(claim, line);
        claim.setAmount(lines.total(claim.getId()));
        claim.setEditedBy(by.name()); // like its opener, may no longer settle or approve it
        repository.update(claim);
    }
}
