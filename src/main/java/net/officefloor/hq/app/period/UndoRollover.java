package net.officefloor.hq.app.period;

import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class UndoRollover {
    // Only the sheet's last rollover, not undone yet, by the steward who made it, on the sheet-today it was made, and
    // only while no claim belongs to a period it generated (its date or due-by within one, whatever its status). Its
    // periods are removed and the opening balances it set are taken back; then ROLLOVER_UNDONE fy=<old FY>.
    public void undo(@PathVariable("sheetId") String sheetId, Rollovers rollovers, Periods periods, Claims claims,
            Settings settings, Sheets sheets, ServerHttpConnection connection, Audit audit,
            Out<MadeRollover> undoneOut)
            throws PeriodRefusedException {
        String sheet = sheetId.trim();
        String user = Audit.user(connection).trim();
        MadeRollover last = rollovers.last(sheet);
        if (last == null || last.undone()) {
            throw new PeriodRefusedException("There is no rollover to undo.");
        }
        if (!Settings.STEWARD.equals(settings.role(sheet, user)) || !last.madeBy().equals(user)) {
            throw new PeriodRefusedException("Only the steward who rolled the year over can undo it.");
        }
        if (!last.madeOn().equals(sheets.today(sheet))) {
            throw new PeriodRefusedException("A rollover can be undone only on the day it was made.");
        }
        List<Period> generated = last.periods().stream().map(name -> periods.find(sheet, name))
                .filter(Objects::nonNull).toList();
        for (Claim claim : claims.list(sheet)) {
            if (generated.stream().anyMatch(p -> p.contains(claim.date())
                    || (claim.dueBy() != null && p.contains(claim.dueBy())))) {
                throw new PeriodRefusedException("A claim belongs to a period the rollover made.");
            }
        }
        if (!rollovers.undo(last.id())) {
            throw new PeriodRefusedException("The rollover was already undone.");
        }
        last.openings().forEach((period, byPerson) -> byPerson.forEach(
                (personId, amount) -> periods.takeOpening(sheet, period, personId, amount)));
        generated.forEach(p -> periods.remove(sheet, p.name()));
        audit.record(Audit.line("ROLLOVER_UNDONE", sheet, user, "fy=" + last.fy()));
        undoneOut.set(last);
    }
}
