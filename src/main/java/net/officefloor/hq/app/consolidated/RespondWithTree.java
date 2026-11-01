package net.officefloor.hq.app.consolidated;

import java.util.List;
import net.officefloor.hq.app.person.People;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithTree {
    public void respond(@PathVariable("sheetId") String sheetId, People people,
            ObjectResponse<List<String>> response) {
        response.send(people.treeOf(sheetId).stream().sorted().toList());
    }
}
