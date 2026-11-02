package net.officefloor.hq.app.period;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Soft-close an open period, by a role the sheet's permission matrix allows to close one, with {@link PeriodCloser}:
 *  its due-total and each person's due-total for it are fixed as a close fixes them, but claims dated inside it can
 *  still be added, edited, opened, settled and voided. A closed or soft-closed period cannot be soft-closed. */
public class SoftClosePeriod {
    public void softClose(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
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
        if (found.softClosed()) {
            throw new BadRequestException("the period " + found.name() + " is already soft-closed");
        }
        period.set(closer.close(id, by, found, false, true));
    }
}
