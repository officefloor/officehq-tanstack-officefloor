package net.officefloor.hq.app.note;

import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The sheet's notes, newest first; read as of a date, those added while its today was on or before that date (a preview reads them all). */
public class LoadNotes {
    public void load(@Val Sheet sheet, NoteRepository repository, Out<Notes> notes) {
        AsOfView asOf = AsOfView.current();
        notes.set(new Notes(asOf == null || asOf.preview() ? repository.findAll(sheet.id()) : repository.findUpTo(sheet.id(), asOf.date())));
    }
}
