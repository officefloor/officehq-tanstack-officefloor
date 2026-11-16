package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Checks a transfer: only a steward of the person's sheet transfers, never a retired person (whatever the date), and only
 *  to another sheet, not archived. When their name or an alias is already a name or alias there (case ignored), they merge
 *  into that person — refused when their names lead to more than one person there, or that person is retired; otherwise
 *  none of their contact may be taken there. Takes the other sheet's figures before it. */
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
        Person into = null;
        List<String> names = new ArrayList<>(List.of(person.name()));
        names.addAll(person.aliases());
        for (String name : names) {
            Person match = people.findByName(to, name);
            if (match == null) {
                continue;
            }
            if (into != null && into.id() != match.id()) {
                throw new InvalidPersonException("The names of " + person.name() + " are on more than one person of " + to);
            }
            into = match;
        }
        if (into != null && into.retired()) {
            throw new InvalidPersonException("A retired person cannot be merged");
        }
        if (into == null && person.contact() != null && people.contactTaken(to, person.contact(), 0)) {
            throw new InvalidPersonException("The contact of " + person.name() + " is already on the sheet " + to);
        }
        transfer.set(new PersonTransfer(person, to, request.by(), rollup.take(to), into));
    }
}
