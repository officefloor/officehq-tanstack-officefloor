package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Undoing a transfer: only a steward of this sheet may, only for a person a transfer brought here, and only on the
 * sheet's today they arrived. Publishes the transfer back to the sheet they came from, which {@link TransferPerson}
 * then makes by exactly the transfer rules.
 */
public class CheckTransferUndo {
    public void check(@Val Person person, @Val ChangedBy by, RoleRepository roles, Out<TransferInput> transfer)
            throws BadRequestException {
        if (!Role.STEWARD.equals(roles.roleOf(person.getSheetId(), by.name()))) {
            throw new BadRequestException("Only a steward may undo a transfer");
        }
        if (person.getOrigin() == null || person.getArrivedOn() == null) {
            throw new BadRequestException("This person did not arrive by transfer");
        }
        if (!person.getArrivedOn().equals(person.getToday())) {
            throw new BadRequestException("A transfer can only be undone on the day the person arrived");
        }
        transfer.set(new TransferInput(person.getOrigin()));
    }
}
