package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildClaim {
    public void build(@Val ClaimInput input, @Val Sheet sheet, @Val Person person, Out<Claim> claimOut) {
        claimOut.set(new Claim(null, sheet.id(), person.getId(), input.amount(),
                input.date() == null ? sheet.today() : input.date(), input.note(),
                ClaimStatus.DRAFT, null, null, null));
    }
}
