package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's descendants: the sheets naming it as their parent, and theirs, and so on. */
public class LoadDescendants {
    public void service(@PathVariable("sheetId") String sheetId, Sheets sheets,
            ObjectResponse<DescendantsResponse> response) {
        response.send(new DescendantsResponse(sheets.descendants(sheetId)));
    }
}
