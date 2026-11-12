package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithNotes {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, NoteStore store,
            ObjectResponse<List<NoteResponse>> response) {
        response.send(store.list(sheetId.trim(), History.date(asOf)));
    }
}
