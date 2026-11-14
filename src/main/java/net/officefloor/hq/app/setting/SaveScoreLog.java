package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveScoreLog {
    public void save(@Val ScoreLog scoreLog, @Val Sheet sheet, SettingRepository repository) {
        if (scoreLog.given()) {
            repository.saveScoreLog(sheet.id(), scoreLog.on());
        }
    }
}
