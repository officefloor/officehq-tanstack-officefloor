package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) undoing a transfer unless a steward undoes it for a person who arrived on this sheet by transfer, on
 * the sheet's today they arrived. Publishes the transfer back to the sheet they came from, for the transfer's own
 * checks.
 */
public class CheckTransferUndo {
    public void check(@RequestParam("user") String user, @Val Person person, SettingRepository settings,
            PersonRepository people, Out<TransferRequest> transfer) throws InvalidPersonException {
        Settings sheet = settings.find(person.sheetId());
        if (sheet.roleOf(user) != Role.STEWARD) {
            throw new InvalidPersonException("Only a steward may undo a transfer.");
        }
        String origin = people.origin(person.sheetId(), person.id());
        LocalDate arrived = people.arrivedOn(person.sheetId(), person.id());
        if (origin == null || arrived == null) {
            throw new InvalidPersonException("This person did not arrive by transfer.");
        }
        if (!arrived.equals(sheet.today())) {
            throw new InvalidPersonException("A transfer can only be undone on the day the person arrived.");
        }
        transfer.set(new TransferRequest(origin));
    }
}
