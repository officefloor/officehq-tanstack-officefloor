package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListNotes {
    public void list(@PathVariable("sheetId") String sheetId, NoteRepository notes,
            ObjectResponse<List<Note>> response) {
        response.send(notes.list(sheetId.trim()));
    }
}
