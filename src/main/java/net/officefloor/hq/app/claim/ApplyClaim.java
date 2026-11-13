package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Claim claim, SheetRepository sheets, Out<Claim> updated) {
        updated.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), input.amount(),
                input.date() != null ? input.date() : sheets.today(claim.sheetId()), input.note(), claim.status(),
                claim.number()));
    }
}
