package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's notes, newest first; with an as-of date, those added while the sheet's today was on or before it. */
public class LoadNotes {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Notes notes,
            ObjectResponse<List<NoteResponse>> response) {
        response.send(notes.list(sheetId, AsOfView.parse(asOf)));
    }
}
