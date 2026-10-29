package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateNote {

    // The text, trimmed, must not be blank. The note is dated the sheet's today.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody NoteRequest body, Sheets sheets,
            ServerHttpConnection connection, Out<Note> note) throws NoteRefusedException {
        String sheet = sheetId.trim();
        String text = body == null || body.text() == null ? "" : body.text().trim();
        if (text.isEmpty()) {
            throw new NoteRefusedException("Give the note's text.");
        }
        note.set(new Note(sheet, text, Audit.user(connection), sheets.today(sheet)));
    }
}
