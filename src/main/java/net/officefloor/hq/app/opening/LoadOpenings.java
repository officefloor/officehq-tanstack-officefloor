package net.officefloor.hq.app.opening;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Each person's opening balance for each period on the sheet that has one. */
public class LoadOpenings {
    public void service(@PathVariable("sheetId") String sheetId, Openings openings,
            ObjectResponse<List<OpeningResponse>> response) {
        response.send(openings.list(sheetId));
    }
}
