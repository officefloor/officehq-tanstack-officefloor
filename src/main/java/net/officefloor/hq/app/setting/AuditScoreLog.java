package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the score log just saved (on, or blank), even when it is the one the sheet had; nothing when left as it is. */
public class AuditScoreLog {
    public void audit(@Val ScoreLog scoreLog, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (scoreLog.given()) {
            audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=score-log value="
                    + (scoreLog.on() ? "on" : ""));
        }
    }
}
