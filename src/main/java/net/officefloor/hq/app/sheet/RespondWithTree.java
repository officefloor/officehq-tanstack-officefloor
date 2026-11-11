package net.officefloor.hq.app.sheet;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The other sheets of the sheet's tree (see {@link SheetRepository#tree}), in id order. */
public class RespondWithTree {
    public void respond(@Val Sheet sheet, SheetRepository repository, ObjectResponse<TreeResponse> response) {
        List<String> sheets = new ArrayList<>(repository.tree(sheet.id()));
        sheets.sort(null);
        response.send(new TreeResponse(sheets));
    }
}
