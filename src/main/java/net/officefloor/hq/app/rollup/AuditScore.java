package net.officefloor.hq.app.rollup;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Records, before the sheet's today changes and while its score log is on, a SCORE line per person in
 * list order with their score as it stood on the day being left.
 */
public class AuditScore {
    public void audit(@Val Sheet sheet, @Val Rollup before, HttpServletRequest request, Audit audit,
            SettingsRepository settings) {
        if (!settings.find(sheet.id()).scoreLog()) return;
        String head = " sheet=" + sheet.id() + " by=" + Audit.user(request);
        before.people().forEach((id, figures) ->
                audit.record("SCORE" + head + " name=" + figures.name() + " score=" + figures.score()));
    }
}
