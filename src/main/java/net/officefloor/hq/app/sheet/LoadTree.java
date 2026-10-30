package net.officefloor.hq.app.sheet;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The other sheets of the sheet's tree (its root and every descendant of the root), by sheet id. */
public class LoadTree {
    public void service(@PathVariable("sheetId") String sheetId, Sheets sheets,
            ObjectResponse<TreeResponse> response) {
        List<String> others = new ArrayList<>(sheets.tree(sheetId));
        others.remove(sheetId);
        others.sort(null);
        response.send(new TreeResponse(others));
    }
}
