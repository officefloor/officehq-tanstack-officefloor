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
 *  but a steward, a retired person, this sheet's own id, an archived sheet, a contact of the person
 *  already on the other sheet, and a name or alias matching more than one person (or a retired one)
 *  there. One person matching is merged into. Publishes the person as they arrived (or merged into). */
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
        Person into = matching(people, person, to);
        if (into != null) {
            into = people.lock(to, into.getId());
            if (into.isRetired()) {
                throw new InvalidSheetRequestException("A retired person cannot be merged into");
            }
        } else if (person.getContact() != null && people.contactTaken(to, person.getContact(), null)) {
            throw new InvalidSheetRequestException("The other sheet already has this contact");
        }
        Snapshot toBefore = rollup.snapshot(to);
        long arrivedId = transfers.transfer(person, to, into);
        Person arrived = people.find(to, arrivedId);
        personOut.set(arrived);
        transferOut.set(new Transfer(person.getName(), sheetId, to, request.user(), toBefore,
                into != null ? arrived : null));
    }

    /** The one person on sheet `to` with the person's name or one of their aliases, as names are
     *  compared, or null when there is none. Refused when they match more than one person there. */
    private static Person matching(People people, Person person, String to) throws InvalidSheetRequestException {
        java.util.Map<Long, Person> found = new java.util.LinkedHashMap<>();
        for (String name : java.util.stream.Stream.concat(java.util.stream.Stream.of(person.getName()),
                person.getAliases().stream()).toList()) {
            Person there = people.named(to, name);
            if (there != null) found.putIfAbsent(there.getId(), there);
        }
        if (found.size() > 1) {
            throw new InvalidSheetRequestException("The other sheet has more than one person by these names");
        }
        return found.values().stream().findFirst().orElse(null);
    }
}
