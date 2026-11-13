package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Finds the last merge into the person, refusing when there is none (or it is undone already), it was made on a
 *  sheet-today other than this sheet's today or by another user, or the merged person's name is now taken. */
public class PrepareMergeUndo {
    public void prepare(@Val MergeUndoRequest request, @Val Person person, PersonMergeStore merges, SheetStore sheets,
            Out<PersonMergeStore.Merge> merge) throws InvalidSheetException {
        PersonMergeStore.Merge last = merges.last(person);
        if (last == null || last.undone()) {
            throw new InvalidSheetException(person.getName() + " has no merge to undo");
        }
        if (last.on() == null || !last.on().equals(sheets.today(person.getSheetId()))) {
            throw new InvalidSheetException("A merge can be undone only on the day it was made");
        }
        if (!last.by().equals(request.user())) {
            throw new InvalidSheetException("Only " + last.by() + " may undo this merge");
        }
        if (merges.nameTaken(person.getSheetId(), last)) {
            throw new InvalidSheetException("The name " + last.name() + " is taken");
        }
        merge.set(last);
    }
}
