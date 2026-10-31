package net.officefloor.hq.app.settings;

import net.officefloor.plugin.variable.Val;

public class SaveDelegation {
    public void save(@Val Delegation delegation, Settings settings) {
        settings.delegate(delegation);
    }
}
