package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListCrews {
    public void list(@PathVariable("sheetId") String sheetId, CrewRepository crews,
            ObjectResponse<List<Crew>> response) {
        response.send(crews.list(sheetId.trim()));
    }
}
