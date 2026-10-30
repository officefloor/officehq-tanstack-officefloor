package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's crews, in the order added. */
public class LoadCrews {
    public void service(@PathVariable("sheetId") String sheetId, Crews crews, ObjectResponse<List<CrewResponse>> response) {
        response.send(crews.list(sheetId));
    }
}
