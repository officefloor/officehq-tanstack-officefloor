package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.watch.WatchNotices;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Records the settle of the claim a part-settle split off, after the split's own lines: its status
 * line, a line per watcher of its person once it is settled, then the figures the settle moved.
 */
public class AuditSplitSettled {
    public void audit(@Val Sheet sheet, @Val Split split, @Val PartSettle part, PersonRepository people,
            WatchRepository watches, SheetRepository sheets, ClaimRepository claims, RateRepository rates,
            SettingsRepository settings, HttpServletRequest request, Audit audit) {
        String user = Audit.user(request);
        Claim made = split.made();
        audit.record(AuditClaimStatus.line(sheet, made, user, people));
        WatchNotices.lines(sheet, made, user, people, watches).forEach(audit::record);
        Rollup after = Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates, settings.find(sheet.id()));
        after.changesSince(part.split(), sheet.id(), user).forEach(audit::record);
    }
}
