package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Before the sheet's today changes, while its score log is on, records one SCORE line per person, in list order, with
 * their score as it stood on the day being left.
 */
public class AuditScores {
    public void audit(@Val RollupBefore before, @PathVariable("sheetId") String sheetId,
            @RequestParam("user") String user, SettingRepository settings, Audit audit) {
        if (!settings.scoreLog(sheetId)) {
            return;
        }
        for (Rollup.Figures figures : before.rollup().people().values()) {
            audit.record("SCORE sheet=" + sheetId + " by=" + user + " name=" + figures.name()
                    + " score=" + figures.score());
        }
    }
}
