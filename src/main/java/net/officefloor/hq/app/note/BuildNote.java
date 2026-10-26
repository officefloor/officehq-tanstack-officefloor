package net.officefloor.hq.app.note;

import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the note, dated the sheet's today. */
public class BuildNote {
    public void build(@Val SheetResponse sheet, @Val NoteInput input, @Val ChangedBy by, SheetRepository sheets,
            NoteRepository repository, Out<Note> note) {
        Note added = new Note(sheet.id(), sheets.today(sheet.id()), by.name(), input.text());
        repository.insert(added);
        note.set(added);
    }
}
