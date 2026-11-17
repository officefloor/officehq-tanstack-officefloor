package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Checks undoing the last merge into a person: only a steward undoes it, only the one who made it, only on the sheet's
 *  today it was made, and only while it is not undone already; a person never merged into, or one whose merged person's
 *  name is now another person's, is refused. */
public class CheckMergeUndo {
    public void check(@Val Person person, @Val MergeUndoRequest request, RoleRepository roles, SheetRepository sheets,
            MergeUndoer undoer, Out<PersonMergeUndo> undo) throws InvalidPersonException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(person.sheetId(), request.by()))) {
            throw new InvalidPersonException("Only a steward undoes a merge");
        }
        MergeUndoer.LastMerge last = undoer.last(person.sheetId(), person.id());
        if (last == null || last.undone()) {
            throw new InvalidPersonException("No merge into " + person.name() + " to undo");
        }
        if (!last.by().equals(request.by())) {
            throw new InvalidPersonException("Only the user who made the merge undoes it");
        }
        if (!last.madeOn().equals(sheets.today(person.sheetId()))) {
            throw new InvalidPersonException("A merge is undone only on the day it was made");
        }
        String name = undoer.fromName(last.id());
        if (undoer.nameHeld(person.sheetId(), name)) {
            throw new InvalidPersonException(name + " is another person's name now");
        }
        undo.set(new PersonMergeUndo(person, last, name, request.by()));
    }
}
