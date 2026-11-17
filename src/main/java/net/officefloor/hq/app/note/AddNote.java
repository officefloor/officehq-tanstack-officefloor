package net.officefloor.hq.app.note;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the note on the sheet, dated the sheet's today. */
public class AddNote {
    public void add(@PathVariable("sheetId") String sheetId, @Val NoteValues values, NoteRepository notes,
            SheetRepository sheets, Out<Note> added) {
        String sheet = sheetId.trim();
        added.set(notes.add(sheet, values.text(), values.by(), sheets.today(sheet)));
    }
}
