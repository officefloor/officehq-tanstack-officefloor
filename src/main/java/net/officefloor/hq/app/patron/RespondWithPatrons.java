package net.officefloor.hq.app.patron;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPatrons {
    public void respond(@PathVariable("sheetId") String sheetId, Patrons patrons,
            ObjectResponse<List<PatronResponse>> response) {
        response.send(patrons.list(sheetId.trim()));
    }
}
