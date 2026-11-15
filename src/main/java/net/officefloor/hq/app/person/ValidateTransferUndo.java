package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward undoes the transfer that brought the person to this sheet, on the sheet's today they
 *  arrived: publishes the transfer back to the sheet they came from (see TransferPerson). Refused for
 *  anyone else, a person who did not arrive by transfer, and any later day. */
public class ValidateTransferUndo {
    public void validate(@RequestParam(name = "by", required = false) String by, @Val Person person,
            RoleRepository roles, SheetRepository sheets, Out<TransferRequest> back) throws InvalidPersonException {
        String sheetId = person.getSheetId();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, by == null ? "" : by))) {
            throw new InvalidPersonException("Only a steward can undo a transfer");
        }
        if (person.getOrigin() == null || person.getArrivedOn() == null) {
            throw new InvalidPersonException("The person did not arrive by transfer");
        }
        if (!sheets.today(sheetId).toString().equals(person.getArrivedOn())) {
            throw new InvalidPersonException("A transfer can be undone only on the day they arrived");
        }
        back.set(new TransferRequest(person.getOrigin()));
    }
}
