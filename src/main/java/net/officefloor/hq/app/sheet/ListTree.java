package net.officefloor.hq.app.sheet;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The ids of the sheets of the sheet's tree: its root first, then the root's descendants, nearest first. */
public class ListTree {
    public void list(@PathVariable("sheetId") String sheetId, SheetRepository sheets,
            ObjectResponse<List<String>> response) {
        response.send(sheets.tree(sheetId.trim()));
    }
}
