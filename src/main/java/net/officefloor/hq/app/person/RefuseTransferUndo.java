package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Refuse undoing the transfer unless a steward asks on the sheet-today the loaded person arrived on this sheet by
 *  transfer; otherwise it goes ahead as a transfer back to the sheet they came from. */
public class RefuseTransferUndo {
    public void refuse(@Val TransferUndoRequest request, @Val Person loaded, PersonRepository people,
            RoleRepository roles, SheetRepository sheets, Out<TransferInput> transfer) throws InvalidPersonException {
        String sheetId = loaded.sheetId();
        if (!Role.STEWARD.equals(roles.of(sheetId, request.user()))) {
            throw new InvalidPersonException("Only a steward may undo a transfer.");
        }
        LocalDate arrived = people.arrivedOn(sheetId, loaded.id());
        if (loaded.origin() == null || arrived == null) {
            throw new InvalidPersonException(loaded.name() + " did not arrive here by transfer.");
        }
        if (!arrived.equals(sheets.today(sheetId))) {
            throw new InvalidPersonException(loaded.name() + "'s transfer can be undone only on the day they arrived ("
                    + arrived + ").");
        }
        transfer.set(new TransferInput(request.user(), loaded.origin()));
    }
}
