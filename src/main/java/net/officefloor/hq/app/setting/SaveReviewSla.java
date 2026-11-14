package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveReviewSla {
    public void save(@Val ReviewSla days, @Val Sheet sheet, SettingRepository repository) {
        if (days.given()) {
            repository.saveReviewSla(sheet.id(), days.value());
        }
    }
}
