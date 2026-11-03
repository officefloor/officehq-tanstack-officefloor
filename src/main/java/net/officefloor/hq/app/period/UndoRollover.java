package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Undo the sheet's last rollover, by the steward who made it and on the same sheet-today only: remove the periods it
 *  generated and take back the opening balances it added, then write ROLLOVER_UNDONE. Refused when there is no
 *  rollover to undo, by anyone else, on a later day, or when a claim (in any status) now belongs to a period it
 *  generated (by its date or due-by). */
public class UndoRollover {

    public void undo(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            PeriodRepository repository, SettingsRepository settings, Audit audit, Out<GenerateResult> result)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        String role = settings.role(id, by);
        if (!SettingsRepository.STEWARD.equals(role)) {
            throw new BadRequestException("a " + role + " cannot undo a rollover");
        }
        String[] last = repository.lastRollover(id);
        if (last == null) {
            throw new BadRequestException("there is no rollover to undo");
        }
        if (!last[0].equals(by)) {
            throw new BadRequestException("only " + last[0] + " can undo the last rollover");
        }
        String today = repository.today(id);
        if (last[1] == null ? today != null : !last[1].equals(today)) {
            throw new BadRequestException("the last rollover can be undone only on the day it was made (" + last[1] + ")");
        }
        List<Long> periods = repository.lastRolloverPeriods(id);
        if (repository.claimsIn(id, periods)) {
            throw new BadRequestException("a claim now belongs to a period the rollover generated");
        }
        repository.undoRollover(id);
        audit.record("ROLLOVER_UNDONE sheet=" + id + " by=" + by + " fy=" + last[2]);
        result.set(new GenerateResult(periods.size()));
    }
}
