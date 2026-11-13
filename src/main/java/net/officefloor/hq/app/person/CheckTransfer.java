package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) a transfer unless a steward transfers a person who is not retired to another sheet, not archived,
 * where no one on any other sheet of its tree (but their own entries) has their name or any of their aliases as a name
 * or alias. When someone on the other sheet has their name or one of their aliases as a name or alias, the person is
 * merged into them on arrival, refused when that is more than one person or they are retired; otherwise no one there
 * may have their contact. Publishes the transfer.
 */
public class CheckTransfer {
    public void check(@RequestParam("user") String user, @Val Person person, @Val TransferRequest request,
            SettingRepository settings, SheetRepository sheets, PersonRepository people, NameTree tree,
            Out<Transfer> transfer)
            throws InvalidPersonException {
        if (settings.find(person.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidPersonException("Only a steward may transfer people.");
        }
        String to = request.to();
        if (to.equals(person.sheetId())) {
            throw new InvalidPersonException("A person cannot be transferred to their own sheet.");
        }
        if (person.retired()) {
            throw new InvalidPersonException("A retired person cannot be transferred.");
        }
        if (sheets.isArchived(to)) {
            throw new InvalidPersonException("An archived sheet cannot be changed.");
        }
        List<String> names = new ArrayList<>();
        names.add(person.name());
        names.addAll(people.aliases(person.sheetId(), person.id()));
        Set<String> treeTaken = tree.taken(to, person.id());
        Person into = null;
        for (String name : names) {
            if (NameTree.clashing(treeTaken, name, List.of())) {
                throw new InvalidPersonException("A sheet related to the other sheet already has " + name + ".");
            }
            Person found = people.findByName(to, name);
            if (found != null) {
                if (into != null && !into.id().equals(found.id())) {
                    throw new InvalidPersonException("More than one person on the other sheet has their names.");
                }
                into = found;
            }
        }
        if (into != null && into.retired()) {
            throw new InvalidPersonException("A retired person cannot be merged.");
        }
        if (into == null && person.contact() != null && people.contactTaken(to, person.contact(), null)) {
            throw new InvalidPersonException("The other sheet already has that contact.");
        }
        transfer.set(new Transfer(person, to, into));
    }
}
