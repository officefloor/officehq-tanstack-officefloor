package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the delegation. */
public class SaveDelegation {
    public void save(@PathVariable("sheetId") String sheetId, @Val DelegationValues values,
            DelegationRepository delegations, Out<Delegation> delegation) {
        delegation.set(delegations.add(sheetId.trim(), values));
    }
}
