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
 * where no one has their name or any of their aliases as a name or alias, nor on any other sheet of its tree (but
 * their own entries), nor their contact; publishes the transfer.
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
        for (String name : names) {
            if (people.nameTaken(to, name, null)) {
                throw new InvalidPersonException("The other sheet already has " + name + ".");
            }
            if (NameTree.clashing(treeTaken, name, List.of())) {
                throw new InvalidPersonException("A sheet related to the other sheet already has " + name + ".");
            }
        }
        if (person.contact() != null && people.contactTaken(to, person.contact(), null)) {
            throw new InvalidPersonException("The other sheet already has that contact.");
        }
        transfer.set(new Transfer(person, to));
    }
}
