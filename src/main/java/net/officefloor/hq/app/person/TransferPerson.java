package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A steward transfers the person to another sheet (see {@link Transfers#transfer}). Refused: anyone
 *  but a steward, a retired person, this sheet's own id, an archived sheet, and a name, alias or
 *  contact of the person already on the other sheet. Publishes the person as they arrived. */
public class TransferPerson {
    public void transfer(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val TransferRequest request, People people, Transfers transfers, Settings settings, Sheets sheets,
            Rollup rollup, Out<Person> personOut, Out<Transfer> transferOut)
            throws PersonNotFoundException, InvalidSheetRequestException {
        String to = request.to().trim();
        if (settings.role(sheetId, request.user()) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can transfer people");
        }
        if (to.equals(sheetId)) {
            throw new InvalidSheetRequestException("A person cannot be transferred to their own sheet");
        }
        Person person = people.lock(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        if (person.isRetired()) {
            throw new InvalidSheetRequestException("A retired person cannot be transferred");
        }
        if (sheets.archived(to)) {
            throw new InvalidSheetRequestException("The other sheet is archived");
        }
        if (people.nameTaken(to, person.getName(), null)
                || person.getAliases().stream().anyMatch(alias -> people.nameOrAliasTaken(to, alias))
                || (person.getContact() != null && people.contactTaken(to, person.getContact(), null))) {
            throw new InvalidSheetRequestException("The other sheet already has this name, alias or contact");
        }
        Snapshot toBefore = rollup.snapshot(to);
        long arrivedId = transfers.transfer(person, to);
        personOut.set(people.find(to, arrivedId));
        transferOut.set(new Transfer(person.getName(), sheetId, to, request.user(), toBefore));
    }
}
