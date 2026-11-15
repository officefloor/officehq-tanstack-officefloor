package net.officefloor.hq.app.note;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class AddNote {
    public void add(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val NoteRequest request, SheetRepository sheets, NoteRepository notes, Out<Note> note) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        note.set(notes.add(id, request.text(), user, sheets.today(id)));
    }
}
