package net.officefloor.hq.app.sheet;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The ids of the sheet's descendants: its children (the sheets naming it as their parent), theirs, ... */
public class ListDescendants {
    public void list(@PathVariable("sheetId") String sheetId, SheetRepository sheets,
            ObjectResponse<List<String>> response) {
        response.send(sheets.descendants(sheetId.trim()));
    }
}
