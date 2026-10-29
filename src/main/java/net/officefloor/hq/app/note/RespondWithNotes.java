package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithNotes {
    public void respond(@PathVariable("sheetId") String sheetId, Notes notes,
            ObjectResponse<List<NoteResponse>> response) {
        response.send(notes.list(sheetId.trim()));
    }
}
