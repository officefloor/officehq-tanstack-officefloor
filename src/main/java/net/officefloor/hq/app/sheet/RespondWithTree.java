package net.officefloor.hq.app.sheet;

import java.util.List;
import net.officefloor.hq.app.person.TreeNames;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The ids of the other sheets of the sheet's tree (its ancestors and descendants), by id. */
public class RespondWithTree {
    public void respond(@Val Sheet sheet, TreeNames trees, ObjectResponse<List<String>> response) {
        response.send(trees.treeOf(sheet.getId()).stream().sorted().toList());
    }
}
