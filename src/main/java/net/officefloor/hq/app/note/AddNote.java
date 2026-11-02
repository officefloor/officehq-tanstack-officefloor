package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a note to the sheet, dated the sheet's today; refused for a blank text. */
public class AddNote {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody NoteRequest request, Notes notes, SheetClock clock, Audit audit) throws InvalidNoteException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidNoteException("A note needs some text");
        }
        notes.add(sheet, clock.today(sheet), user, text);
        audit.record("NOTE_ADDED sheet=" + sheet + " by=" + user + " text=" + text);
    }
}
