package net.officefloor.hq.app.opening;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every opening balance on the sheet. */
public class RespondWithOpenings {
    public void respond(@PathVariable("sheetId") String sheetId, OpeningStore store,
            ObjectResponse<List<OpeningResponse>> response) {
        response.send(store.list(sheetId.trim()));
    }
}
