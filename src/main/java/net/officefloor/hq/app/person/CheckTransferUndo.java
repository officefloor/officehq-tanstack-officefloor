package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Checks undoing a transfer: only a steward of the sheet undoes it, only for a person who arrived here by a transfer, and
 *  only on the sheet's today they arrived. Then asks for the transfer back to the sheet they came from, which the transfer
 *  rules check in turn (see CheckTransfer). */
public class CheckTransferUndo {
    public void check(@Val Person person, @Val TransferUndoRequest request, RoleRepository roles, SheetRepository sheets,
            PersonRepository people, Out<TransferRequest> transfer) throws InvalidPersonException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(person.sheetId(), request.by()))) {
            throw new InvalidPersonException("Only a steward undoes a transfer");
        }
        LocalDate on = people.arrivedOn(person.sheetId(), person.id());
        if (person.origin() == null || on == null) {
            throw new InvalidPersonException(person.name() + " did not arrive by a transfer");
        }
        if (!on.equals(sheets.today(person.sheetId()))) {
            throw new InvalidPersonException("A transfer is undone only on the day the person arrived");
        }
        transfer.set(new TransferRequest(person.origin(), request.by()));
    }
}
