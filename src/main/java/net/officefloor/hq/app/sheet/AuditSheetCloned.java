package net.officefloor.hq.app.sheet;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Records the clone on this sheet, then the clone's first lines: where it was cloned from, followed by
 * its rollup lines for every person and its totals. Runs after every rule has passed, so a refused
 * clone writes nothing.
 */
public class AuditSheetCloned {
    public void audit(@Val Sheet sheet, @Val Clone clone, PersonRepository people, ClaimRepository claims,
            RateRepository rates, SettingsRepository settings, HttpServletRequest request, Audit audit) {
        String user = Audit.user(request);
        String to = clone.to().id();
        audit.record("SHEET_CLONE_MADE sheet=" + sheet.id() + " by=" + user + " to=" + to);
        audit.record("SHEET_CLONED sheet=" + to + " by=" + user + " from=" + sheet.id());
        Rollup none = new Rollup(Map.of(), "", "", "", null);
        Rollup.of(to, clone.to().today(), people, claims, rates, settings.find(to))
                .changesSince(none, to, user).forEach(audit::record);
    }
}
