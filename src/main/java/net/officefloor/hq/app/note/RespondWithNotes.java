package net.officefloor.hq.app.note;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's notes, newest first (as of a date (?asOf=): only those added while the sheet's today was on or before
 *  it). */
public class RespondWithNotes {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, NoteRepository repository,
            ObjectResponse<List<NoteResponse>> response) {
        LocalDate end = AsOf.parse(asOf);
        response.send(repository.list(sheetId.trim()).stream()
                .filter(n -> end == null || n.on() == null || !n.on().isAfter(end))
                .map(NoteResponse::from).toList());
    }
}
