package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The ids of every sheet of the sheet's whole tree, the sheet among them: the topmost ancestor and all its
 *  descendants (named now). */
public class RespondWithWholeTree {
    public void respond(@PathVariable("sheetId") String sheetId, SettingStore store,
            ObjectResponse<List<String>> response) {
        response.send(store.wholeTree(sheetId.trim()));
    }
}
