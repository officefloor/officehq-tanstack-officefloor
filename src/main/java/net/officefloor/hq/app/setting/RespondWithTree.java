package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The ids of the other sheets of the sheet's tree: its ancestors, then its descendants, among those that exist. */
public class RespondWithTree {
    public void respond(@PathVariable("sheetId") String sheetId, SettingStore store,
            ObjectResponse<List<String>> response) {
        response.send(store.tree(sheetId.trim()));
    }
}
