package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;

public class SaveDelegation {
    public void save(@Val Delegation delegation, SettingRepository repository) {
        repository.saveDelegation(delegation);
    }
}
