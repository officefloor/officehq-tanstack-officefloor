package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's notes, newest first. Read as of a date (see AsOf), only those added while the sheet's today was on or
 *  before it. */
public class RespondWithNotes {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, NoteRepository notes,
            ObjectResponse<List<Note>> response) {
        response.send(notes.list(sheetId.trim(), AsOf.parse(asOf)));
    }
}
