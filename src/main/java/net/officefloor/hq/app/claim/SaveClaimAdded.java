package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SaveClaimAdded {
    public void save(@Val ClaimInput input, @Val Person person, ClaimStore store, Out<Claim> claim) {
        claim.set(store.add(person.getSheetId(), person.getId(), input.amount(), input.date(), input.note()));
    }
}
