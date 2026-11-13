package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SavePolicyDate {
    public void save(@Val PolicyDate policyDate, @Val Sheet sheet, SettingRepository repository) {
        if (policyDate.given()) {
            repository.savePolicyDate(sheet.id(), policyDate.value());
        }
    }
}
