package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class UndoMerge {
    // Only the steward who made the last merge into the person undoes it, and only on the sheet-today it was made: the
    // merged person is back with the claims and aliases that went with them.
    public void undo(@Val Person person, People people, Settings settings, Sheets sheets, ServerHttpConnection connection,
            Out<MergeUndone> undoneOut, Out<PersonResponse> responseOut) throws PersonMergeRefusedException {
        String user = Audit.user(connection).trim();
        if (!Settings.STEWARD.equals(settings.role(person.sheetId(), user))) {
            throw new PersonMergeRefusedException("Only a steward can undo a merge.");
        }
        MergeKept merge = people.lastMerge(person.sheetId(), person.id());
        if (merge == null || merge.undone()) {
            throw new PersonMergeRefusedException("There is no merge into this person to undo.");
        }
        if (!merge.mergedBy().equals(user)) {
            throw new PersonMergeRefusedException("Only the steward who made a merge can undo it.");
        }
        if (!sheets.today(person.sheetId()).equals(merge.mergedOn())) {
            throw new PersonMergeRefusedException("A merge can be undone only on the day it was made.");
        }
        if (people.personNamed(person.sheetId(), merge.name())) {
            throw new PersonMergeRefusedException("Another person now has the name " + merge.name() + ".");
        }
        people.undoMerge(person.sheetId(), merge, person.id());
        Person restored = people.find(person.sheetId(), merge.fromId());
        undoneOut.set(new MergeUndone(restored, person));
        responseOut.set(PersonResponse.from(restored, sheets.today(person.sheetId())));
    }
}
