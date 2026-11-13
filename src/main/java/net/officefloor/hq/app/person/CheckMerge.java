package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) a merge unless a steward merges the person into another person on the sheet, neither of them
 * retired; publishes the merge.
 */
public class CheckMerge {
    public void check(@RequestParam("user") String user, @Val Person person, @Val MergeRequest request,
            SettingRepository settings, PersonRepository people, Out<Merge> merge) throws InvalidPersonException {
        if (settings.find(person.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidPersonException("Only a steward may merge people.");
        }
        Person into = people.find(person.sheetId(), request.into());
        if (into == null) {
            throw new InvalidPersonException("No such person to merge into.");
        }
        if (into.id().equals(person.id())) {
            throw new InvalidPersonException("A person cannot be merged into themselves.");
        }
        if (person.retired() || into.retired()) {
            throw new InvalidPersonException("A retired person cannot be merged.");
        }
        merge.set(new Merge(person, into));
    }
}
