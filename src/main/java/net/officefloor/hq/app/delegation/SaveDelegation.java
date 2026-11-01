package net.officefloor.hq.app.delegation;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveDelegation {
    public void save(@PathVariable("sheet") String sheet, @Val Delegation d, Delegations delegations, Audit audit) {
        delegations.add(sheet, d.by(), d.user(), d.from(), d.to());
        audit.record("DELEGATED sheet=" + sheet + " by=" + d.by() + " user=" + d.user() + " from=" + d.from()
                + " to=" + d.to());
    }
}
