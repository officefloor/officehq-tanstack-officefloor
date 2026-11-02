package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the notes added while the sheet's today (the as-of date, when shown as of one) was on or before it. */
public class RespondWithNotes {
    public void respond(@PathVariable("sheet") String sheet, Notes notes, SheetClock clock,
            ObjectResponse<List<Note>> response) {
        response.send(notes.upTo(sheet, clock.today(sheet)));
    }
}
