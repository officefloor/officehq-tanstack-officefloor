package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records each score threshold just saved (blank: none), even when it is the one the sheet had; nothing when left as it is. */
public class AuditScoreThresholds {
    public void audit(@Val ScoreThresholds thresholds, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        record(thresholds.mid(), "score-mid", sheet, user, audit);
        record(thresholds.high(), "score-high", sheet, user, audit);
    }

    private static void record(ScoreThresholds.Threshold threshold, String key, Sheet sheet, String user, Audit audit) {
        if (threshold.given()) {
            audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=" + key + " value="
                    + (threshold.value() == null ? "" : threshold.value().toString()));
        }
    }
}
