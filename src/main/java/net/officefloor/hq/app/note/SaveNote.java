package net.officefloor.hq.app.note;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Saves the note, dated the sheet's today and by the signed-in user. */
public class SaveNote {
    public void save(@Val NoteInput input, @Val Sheet sheet, HttpServletRequest request, NoteRepository repository,
            Out<Note> noteOut) {
        Note note = new Note(sheet.id(), sheet.today(), Audit.user(request), input.text());
        repository.save(note);
        noteOut.set(note);
    }
}
