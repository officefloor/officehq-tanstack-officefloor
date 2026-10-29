package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class UndoTransfer {
    // Only a steward undoes the transfer that brought a person to this sheet, and only on the sheet-today they arrived:
    // they are transferred back to the sheet they came from, by exactly the transfer rules.
    public void undo(@Val Person person, People people, Claims claims, Settings settings, Sheets sheets,
            Periods periods, Rollup rollup, ServerHttpConnection connection, Out<TransferredPerson> transferredOut,
            Out<PersonResponse> responseOut) throws PersonTransferRefusedException {
        if (!Settings.STEWARD.equals(settings.role(person.sheetId(), Audit.user(connection).trim()))) {
            throw new PersonTransferRefusedException("Only a steward can undo a transfer.");
        }
        if (person.origin() == null) {
            throw new PersonTransferRefusedException("This person did not arrive by transfer.");
        }
        if (!sheets.today(person.sheetId()).equals(people.transferredOn(person.id()))) {
            throw new PersonTransferRefusedException("A transfer can be undone only on the day they arrived.");
        }
        TransferPerson.move(person, person.origin(), people, claims, settings, sheets, periods, rollup, transferredOut,
                responseOut);
    }
}
