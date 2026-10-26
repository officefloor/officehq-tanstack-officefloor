package net.officefloor.hq.app.sheet;

import java.util.List;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The ids of the other sheets of the sheet's tree (see {@link PersonRepository#treeOf}), sorted by id. */
public class RespondWithTree {
    public void respond(@Val SheetResponse sheet, PersonRepository people, ObjectResponse<List<String>> response) {
        response.send(people.treeOf(sheet.id()).stream().sorted().toList());
    }
}
