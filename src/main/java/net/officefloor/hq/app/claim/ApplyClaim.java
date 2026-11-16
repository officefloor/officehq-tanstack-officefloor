package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val Claim claim, @Val ClaimValues values, SheetRepository sheets, Out<Claim> changed) {
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), values.amount(),
                values.date() != null ? values.date() : sheets.today(claim.sheetId()), values.note(), claim.status(),
                claim.number(), claim.due(), claim.openedBy()));
    }
}
