package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) undoing a merge into the person unless the steward who made their last merge (not yet undone) undoes
 * it on the sheet's today it was made, and no one else has taken the merged person's name since; publishes the merge.
 */
public class CheckMergeUndo {
    public void check(@RequestParam("user") String user, @Val Person person, SettingRepository settings,
            MergeRepository merges, PersonRepository people, Out<MergeMade> undo) throws InvalidPersonException {
        Settings sheet = settings.find(person.sheetId());
        if (sheet.roleOf(user) != Role.STEWARD) {
            throw new InvalidPersonException("Only a steward may undo a merge.");
        }
        MergeMade made = merges.last(person.sheetId(), person.id());
        if (made == null) {
            throw new InvalidPersonException("There is no merge into this person to undo.");
        }
        if (!made.by().equals(user)) {
            throw new InvalidPersonException("Only the steward who made the merge may undo it.");
        }
        if (!made.madeOn().equals(sheet.today())) {
            throw new InvalidPersonException("A merge can only be undone on the day it was made.");
        }
        if (people.findAll(person.sheetId()).stream().anyMatch((p) -> p.name().equalsIgnoreCase(made.name()))) {
            throw new InvalidPersonException("Someone else has the merged person's name now.");
        }
        undo.set(made);
    }
}
