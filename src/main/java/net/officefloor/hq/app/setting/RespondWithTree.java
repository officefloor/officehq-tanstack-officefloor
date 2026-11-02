package net.officefloor.hq.app.setting;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the sheet's id then those of every other sheet of its whole tree, by id. */
public class RespondWithTree {
    public void respond(@PathVariable("sheet") String sheet, Settings settings,
            ObjectResponse<List<String>> response) {
        List<String> others = new ArrayList<>(settings.wholeTree(sheet));
        others.remove(sheet);
        others.sort(null);
        List<String> found = new ArrayList<>(List.of(sheet));
        found.addAll(others);
        response.send(found);
    }
}
