package net.officefloor.hq.app.person;

import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A steward undoes the last merge into the person (see {@link People#undoMerge}), on the sheet-today
 *  they made it. Refused: anyone but the steward who made it, any later day, a person with no merge
 *  to undo, and a merged person whose name is now taken on the sheet. Publishes the person restored. */
public class UndoMerge {
    public void undo(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val MergeUndoRequest request, People people, Settings settings, Sheets sheets, Out<Person> personOut,
            Out<MergeUndone> undoneOut) throws PersonNotFoundException, InvalidSheetRequestException {
        if (settings.role(sheetId, request.user()) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can undo a merge");
        }
        Person into = people.lock(sheetId, personId);
        if (into == null) throw new PersonNotFoundException(personId);
        PersonMerge merge = people.lastMergeInto(into);
        if (merge == null) throw new InvalidSheetRequestException("No merge into this person to undo");
        if (!request.user().equals(merge.user())) {
            throw new InvalidSheetRequestException("Only the steward who made the merge can undo it");
        }
        if (!merge.on().equals(sheets.today(sheetId))) {
            throw new InvalidSheetRequestException("A merge can be undone only on the day it was made");
        }
        Person from = people.parkedBy(merge);
        if (from == null) throw new InvalidSheetRequestException("No merge into this person to undo");
        if (people.nameTakenButMerge(sheetId, from.getName(), merge)) {
            throw new InvalidSheetRequestException("The merged person's name is now taken on this sheet");
        }
        people.undoMerge(merge, sheetId);
        Person restored = people.find(sheetId, merge.fromId());
        personOut.set(restored);
        undoneOut.set(new MergeUndone(restored, people.find(sheetId, into.getId()), request.user()));
    }
}
