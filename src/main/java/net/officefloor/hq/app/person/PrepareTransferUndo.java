package net.officefloor.hq.app.person;

import java.time.LocalDate;
import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Undoing the transfer that brought the person here (a steward), on the same sheet's today they
 * arrived: publishes the transfer back to the sheet they came from, which then runs as a transfer does.
 * Anyone else, a later day, or a person who did not arrive by transfer is refused.
 */
public class PrepareTransferUndo {
    public void prepare(@Val Sheet sheet, @Val Person person, PersonRepository people, RoleRepository roles,
            HttpServletRequest request, Out<TransferInput> input) throws StewardOnlyException, InvalidPersonException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), Audit.user(request)))) {
            throw new StewardOnlyException("Only a steward may undo a transfer");
        }
        LocalDate arrived = people.arrivedOn(person);
        String from = person.getOriginSheet();
        if (arrived == null || from == null || from.equals(sheet.id())) {
            throw new InvalidPersonException("That person did not arrive by transfer");
        }
        if (!arrived.equals(sheet.today())) {
            throw new InvalidPersonException("A transfer can only be undone on the day they arrived");
        }
        input.set(new TransferInput(from));
    }
}
