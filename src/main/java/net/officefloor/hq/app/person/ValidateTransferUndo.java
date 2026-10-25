package net.officefloor.hq.app.person;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClerkLimitException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Undoing a transfer: only a steward, only for a person a transfer brought here, and only on the sheet's today they
 *  arrived. Publishes the transfer back to the sheet they came from. */
public class ValidateTransferUndo {
    public void validate(@Val Person person, @Val Sheet sheet, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String userHeader, Out<TransferInput> body)
            throws InvalidPersonException, ClerkLimitException {
        String user = AuditUser.decode(userHeader);
        if (!Role.STEWARD.equals(roles.roleOf(person.getSheetId(), user))) {
            throw new ClerkLimitException("Only a steward can undo a transfer.");
        }
        if (person.getOriginSheet() == null || person.getArrivedOn() == null) {
            throw new InvalidPersonException("The person did not arrive by transfer.");
        }
        if (!person.getArrivedOn().equals(sheet.getToday())) {
            throw new InvalidPersonException("A transfer can only be undone on the day the person arrived.");
        }
        body.set(new TransferInput(person.getOriginSheet()));
    }
}
