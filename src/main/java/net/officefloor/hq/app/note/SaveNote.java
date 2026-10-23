package net.officefloor.hq.app.note;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the note, dated the sheet's today. */
public class SaveNote {
    public void save(@PathVariable("sheetId") String sheetId, @Val NoteInput input, SheetRepository sheets,
            NoteRepository repository, Out<Note> note) {
        String id = sheetId.trim();
        note.set(repository.add(id, input, sheets.today(id)));
    }
}
