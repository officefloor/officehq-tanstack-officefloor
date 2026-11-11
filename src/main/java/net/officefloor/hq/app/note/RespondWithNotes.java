package net.officefloor.hq.app.note;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's notes, newest first. Read as of a date, only those added while the sheet's today was on or before it. */
public class RespondWithNotes {
    public void respond(@Val Sheet sheet, NoteRepository repository, HttpServletRequest request,
            ObjectResponse<NotesResponse> response) {
        LocalDate asOf = AsOf.of(request);
        response.send(new NotesResponse(repository.findAll(sheet.id()).stream()
                .filter(n -> asOf == null || !n.addedOn().isAfter(asOf))
                .map(NoteResponse::from).toList()));
    }
}
