package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveScoreThresholds {
    public void save(@Val ScoreThresholds thresholds, @Val Sheet sheet, SettingRepository repository) {
        if (thresholds.mid().given()) {
            repository.saveScoreMid(sheet.id(), thresholds.mid().value());
        }
        if (thresholds.high().given()) {
            repository.saveScoreHigh(sheet.id(), thresholds.high().value());
        }
    }
}
