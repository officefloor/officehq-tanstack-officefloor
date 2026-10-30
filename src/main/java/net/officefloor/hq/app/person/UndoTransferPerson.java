package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimNumbering;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/**
 * Undoes the transfer that brought the person to this sheet (a steward only, on the sheet's today they arrived, and
 * only for a person who arrived by transfer): they are transferred back to the sheet they came from exactly as a
 * transfer does (its rules and audit lines), then PERSON_TRANSFER_UNDONE is written on this sheet.
 */
public class UndoTransferPerson {
    public void undo(@Val TransferUndoRequest request, @Val Person person, People people, Claims claims,
            ClaimNumbering numbering, Sheets sheets, Settings settings, Audit audit, @Val RollupFigures before,
            Rollup rollup) throws InvalidPersonException {
        String sheetId = person.getSheetId();
        String user = request.user();
        if (!Settings.STEWARD.equals(settings.role(sheetId, user))) {
            throw new InvalidPersonException("Only a steward can undo a transfer");
        }
        String origin = person.getOrigin();
        if (origin == null || person.getArrivedOn() == null) {
            throw new InvalidPersonException("The person did not arrive by transfer");
        }
        if (!person.getArrivedOn().equals(sheets.today(sheetId))) {
            throw new InvalidPersonException("A transfer can be undone only on the day the person arrived");
        }
        TransferPerson.move(person, origin, user, people, claims, numbering, sheets, audit, before, rollup);
        audit.recordOnCommit("PERSON_TRANSFER_UNDONE sheet=" + sheetId + " by=" + user
                + " name=" + person.getName() + " to=" + origin);
        rollup.recordChanges(sheetId, user, before);
    }
}
