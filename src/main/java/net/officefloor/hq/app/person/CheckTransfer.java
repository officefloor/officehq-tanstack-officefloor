package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Checks a transfer: only a steward of the person's sheet transfers, never a retired person (whatever the date), and only
 *  to another sheet, not archived, where none of their name, aliases or contact is already taken (a name or alias against
 *  every name and alias there, case ignored). Takes the other sheet's figures before it. */
public class CheckTransfer {
    public void check(@Val Person person, @Val TransferRequest request, PersonRepository people, RoleRepository roles,
            SheetRepository sheets, Rollup rollup, Out<PersonTransfer> transfer) throws InvalidPersonException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(person.sheetId(), request.by()))) {
            throw new InvalidPersonException("Only a steward transfers people");
        }
        if (person.retired()) {
            throw new InvalidPersonException("A retired person cannot be transferred");
        }
        String to = request.toSheet();
        if (to.equals(person.sheetId())) {
            throw new InvalidPersonException("A person transfers to another sheet");
        }
        if (sheets.archived(to)) {
            throw new InvalidPersonException("The sheet " + to + " is archived");
        }
        if (people.nameTaken(to, person.name(), 0) || person.aliases().stream().anyMatch((a) -> people.nameTaken(to, a, 0))) {
            throw new InvalidPersonException("A name of " + person.name() + " is already on the sheet " + to);
        }
        if (person.contact() != null && people.contactTaken(to, person.contact(), 0)) {
            throw new InvalidPersonException("The contact of " + person.name() + " is already on the sheet " + to);
        }
        transfer.set(new PersonTransfer(person, to, request.by(), rollup.take(to)));
    }
}
