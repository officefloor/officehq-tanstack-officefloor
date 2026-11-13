package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildClaim {
    public void build(@Val ClaimInput input, @Val Person person, SheetRepository sheets, Out<Claim> claim) {
        claim.set(new Claim(null, person.sheetId(), person.id(), input.amount(),
                input.date() != null ? input.date() : sheets.today(person.sheetId()), input.note(), ClaimStatus.DRAFT, null));
    }
}
