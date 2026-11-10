package net.officefloor.hq.app.watch;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Notifies the watchers of a claim's person that it was settled (approved, re-settled). Runs after
 * the claim's own line, before the rollup lines; see {@link WatchNotices}.
 */
public class NotifyWatchers {
    public void send(@Val Sheet sheet, @Val Claim claim, PersonRepository people, WatchRepository watches,
            HttpServletRequest request, Audit audit) {
        WatchNotices.lines(sheet, claim, Audit.user(request), people, watches).forEach(audit::record);
    }
}
