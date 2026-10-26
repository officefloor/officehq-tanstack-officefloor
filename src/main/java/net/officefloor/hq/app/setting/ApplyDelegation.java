package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;

public class ApplyDelegation {
    public void apply(@Val Delegation delegation, Delegations delegations) {
        delegations.save(delegation);
    }
}
