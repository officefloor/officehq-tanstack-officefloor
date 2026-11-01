package net.officefloor.hq.app.note;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithNotes {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf,
            @RequestParam(value = "preview", required = false) String preview, Notes notes, AsOf asOfSheet,
            ObjectResponse<List<NoteResponse>> response) throws InvalidSheetRequestException {
        response.send(asOfSheet.view(sheetId, asOf, preview,
                () -> notes.onSheet(sheetId).stream().map(NoteResponse::from).toList()));
    }
}
