package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's notes, newest first; as of a date, those added while the sheet's today was on or before it. */
public class RespondWithNotes {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<List<NoteResponse>> response) {
        response.send(history.view(sheet.id(), asOf.on(), jdbc -> new NoteRepository(jdbc).list(sheet.id()).stream()
                .filter(n -> asOf.on() == null || !n.addedOn().isAfter(asOf.on()))
                .map(NoteResponse::of).toList()));
    }
}
