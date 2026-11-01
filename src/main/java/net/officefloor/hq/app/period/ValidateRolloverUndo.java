package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Only the steward who made the sheet's last rollover may undo it, and only on the sheet's today it
 *  was made, while no claim (whatever its status) belongs, by its date or its due-by, to a period it
 *  generated. A rollover that generated no periods has nothing to undo. Publishes the rollover. */
public class ValidateRolloverUndo {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RolloverUndoRequest request,
            Settings settings, Sheets sheets, Rollovers rollovers, Periods periods, Claims claims,
            Out<Rollovers.Made> undo) throws InvalidSheetRequestException {
        String user = request == null ? null : request.user();
        if (settings.role(sheetId, user) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward may undo a rollover");
        }
        Rollovers.Made made = rollovers.last(sheetId);
        if (made == null || made.periods().isEmpty()) {
            throw new InvalidSheetRequestException("There is no rollover to undo");
        }
        if (!made.user().equals(user)) {
            throw new InvalidSheetRequestException("Only " + made.user() + ", who made the rollover, may undo it");
        }
        if (!made.madeOn().equals(sheets.today(sheetId))) {
            throw new InvalidSheetRequestException("A rollover can only be undone on the day it was made");
        }
        List<Period> generated = periods.onSheet(sheetId).stream()
                .filter(p -> made.periods().contains(p.name())).toList();
        for (Claim claim : claims.onSheet(sheetId)) {
            for (Period period : generated) {
                if ((claim.getDate() != null && period.contains(claim.getDate()))
                        || (claim.getDueBy() != null && period.contains(claim.getDueBy()))) {
                    throw new InvalidSheetRequestException(
                            "A claim belongs to period " + period.name() + ", which the rollover generated");
                }
            }
        }
        undo.set(made);
    }
}
