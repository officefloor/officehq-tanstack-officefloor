package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's crews in the order added, each with its lead and members. */
public class RespondWithCrews {
    public void respond(@PathVariable("sheetId") String sheetId, CrewRepository crews,
            ObjectResponse<List<Crew>> response) {
        response.send(crews.list(sheetId.trim()));
    }
}
