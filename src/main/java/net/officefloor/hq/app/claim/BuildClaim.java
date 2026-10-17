package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildClaim {
    public void build(@Val Person person, Out<Claim> claimOut) {
        claimOut.set(new Claim(person.getSheetId(), person.getId()));
    }
}
