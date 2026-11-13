package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveTerms {
    public void save(@Val Terms terms, @Val Sheet sheet, SettingRepository repository) {
        if (terms.value() != null) {
            repository.saveTerms(sheet.id(), terms.value());
        }
    }
}
