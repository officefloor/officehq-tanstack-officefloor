package net.officefloor.hq.app.note;

import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the note, dated the sheet's today. */
public class SaveNote {
    public void save(@PathVariable("sheetId") String sheetId, @Val NoteInput note, NoteStore store,
            SheetStore sheets, Out<NoteResponse> saved) {
        String sheet = sheetId.trim();
        saved.set(store.add(sheet, sheets.today(sheet), note.user(), note.text()));
    }
}
