package net.officefloor.hq.app.period;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Close a period, by a role the sheet's permission matrix allows (by default a clerk or a steward), with
 *  {@link PeriodCloser}: fix its due-total as shown now (the dues now, late charges included, of the claims dated in it that count by
 *  the status they had at the end of its last day, a person's
 *  open (OPEN or PENDING) claims dated within one of their holds left out unless bound by a statement,
 *  skipping those with none), so later changes to rates do not alter it; and each person's due-total for it
 *  likewise, for every person on the sheet (0.00 for one with nothing in it). */
public class ClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            ServerHttpConnection connection, PeriodRepository repository, SettingsRepository settings,
            PeriodCloser closer, Out<Period> period) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        Period found = repository.find(id, periodName == null ? "" : periodName.trim());
        if (found == null) {
            throw new BadRequestException("no such period");
        }
        if (!settings.allows(id, "close", by)) {
            throw new BadRequestException("a " + settings.role(id, by) + " cannot close a period");
        }
        if (found.closed()) {
            throw new BadRequestException("the period " + found.name() + " is already closed");
        }
        period.set(closer.close(id, by, found, false));
    }
}
