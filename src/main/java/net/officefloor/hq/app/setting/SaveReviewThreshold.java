package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveReviewThreshold {
    public void save(@Val ReviewThreshold threshold, @Val Sheet sheet, SettingRepository repository) {
        if (threshold.given()) {
            repository.saveDated(sheet.id(), DatedSettings.REVIEW_THRESHOLD,
                    DatedSettings.fromOr(threshold.from(), sheet.today()),
                    threshold.value() == null ? null : threshold.value().toPlainString());
        }
    }
}
