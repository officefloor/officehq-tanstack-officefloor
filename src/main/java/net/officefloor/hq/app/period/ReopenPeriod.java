package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Reopen a closed period, by a steward only: its fixed due-total is dropped, so it totals the
 *  claims in it live again (bound ones at their bound figure). */
public class ReopenPeriod {
    public void reopen(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            ServerHttpConnection connection, PeriodRepository repository, SettingsRepository settings, Audit audit,
            Out<Period> period) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        Period found = repository.find(id, periodName == null ? "" : periodName.trim());
        if (found == null) {
            throw new BadRequestException("no such period");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can reopen a period");
        }
        if (!found.closed()) {
            throw new BadRequestException("the period " + found.name() + " is not closed");
        }
        repository.reopen(id, found.id());
        audit.record("PERIOD_REOPENED sheet=" + id + " by=" + by + " period=" + found.name());
        period.set(new Period(found.id(), found.name(), found.from(), found.to(), false, null, java.util.Map.of()));
    }
}
