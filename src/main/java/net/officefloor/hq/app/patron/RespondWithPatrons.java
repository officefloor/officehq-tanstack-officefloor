package net.officefloor.hq.app.patron;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every patron named on the sheet. */
public class RespondWithPatrons {
    public void respond(@PathVariable("sheetId") String sheetId, PatronRepository patrons,
            ObjectResponse<List<Patron>> response) {
        response.send(patrons.list(sheetId.trim()));
    }
}
