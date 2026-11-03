package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Lock or unlock a period, by a steward only: a locked period cannot be reopened. Locking a locked period, or
 *  unlocking an unlocked one, is refused. */
public class LockPeriod {

    /** Lock or unlock the period named in the path; publishes it as it is afterwards. */
    static void apply(String sheetId, String periodName, boolean lock, ServerHttpConnection connection,
            PeriodRepository repository, SettingsRepository settings, Audit audit, Out<Period> period)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        Period found = repository.find(id, periodName == null ? "" : periodName.trim());
        if (found == null) {
            throw new BadRequestException("no such period");
        }
        String role = settings.role(id, by);
        if (!SettingsRepository.STEWARD.equals(role)) {
            throw new BadRequestException("a " + role + " cannot " + (lock ? "lock" : "unlock") + " a period");
        }
        if (found.locked() == lock) {
            throw new BadRequestException("the period " + found.name() + " is " + (lock ? "already locked" : "not locked"));
        }
        repository.setLocked(id, found.id(), lock);
        audit.record((lock ? "PERIOD_LOCKED" : "PERIOD_UNLOCKED") + " sheet=" + id + " by=" + by + " period=" + found.name());
        period.set(repository.list(id).stream().filter(p -> p.id() == found.id()).findFirst().orElseThrow());
    }

    public void lock(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            ServerHttpConnection connection, PeriodRepository repository, SettingsRepository settings, Audit audit,
            Out<Period> period) throws BadRequestException {
        apply(sheetId, periodName, true, connection, repository, settings, audit, period);
    }
}
