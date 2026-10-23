package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.TodayRequest;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Before the sheet's today changes, while its score log is on: a SCORE line per person, in list order, with their
 *  score on the day being left. */
public class AuditScores {
    public void audit(@PathVariable("sheetId") String sheetId, @Val TodayRequest request, Rollups rollups,
            SettingRepository settings, Audit audit) {
        String id = sheetId.trim();
        if (!settings.scoreLog(id)) {
            return;
        }
        for (Rollups.Score s : rollups.scores(id)) {
            audit.record("SCORE sheet=" + id + " by=" + request.user() + " name=" + s.name() + " score=" + s.score());
        }
    }
}
