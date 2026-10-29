package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithCrews {
    public void respond(@PathVariable("sheetId") String sheetId, Crews crews, ObjectResponse<List<CrewResponse>> response) {
        response.send(crews.list(sheetId.trim()));
    }
}
