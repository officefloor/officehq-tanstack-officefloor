package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the note to the sheet, dated the sheet's today. */
public class SaveNote {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidNote note, Notes notes, Sheets sheets, Audit audit,
            ObjectResponse<List<NoteResponse>> response) {
        notes.add(sheetId, note.text(), note.user(), sheets.today(sheetId));
        audit.recordOnCommit("NOTE_ADDED sheet=" + sheetId + " by=" + note.user() + " text=" + note.text());
        response.send(notes.list(sheetId, null));
    }
}
