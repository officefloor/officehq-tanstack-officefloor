package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Claim claim, @Val ChangedBy by, ClaimRepository repository,
            SheetRepository sheets) {
        claim.setAmount(input.amount());
        claim.setDate(input.date() != null ? input.date() : sheets.today(claim.getSheetId()));
        claim.setNote(input.note());
        claim.setCorrects(input.corrects());
        claim.setRef(input.ref());
        claim.setTags(input.tags());
        claim.setEditedBy(by.name()); // like its opener, may no longer settle or approve it
        repository.update(claim);
    }
}
