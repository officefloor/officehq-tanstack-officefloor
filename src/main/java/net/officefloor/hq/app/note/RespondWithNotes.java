package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's notes, newest first. */
public class RespondWithNotes {
    public void respond(@PathVariable("sheetId") String sheetId, NoteRepository repository,
            ObjectResponse<List<NoteResponse>> response) {
        response.send(repository.list(sheetId.trim()).stream().map(NoteResponse::from).toList());
    }
}
