package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The ids of the sheet's descendants: the sheets naming it as their parent, their children, and so on. */
public class RespondWithDescendants {
    public void respond(@PathVariable("sheetId") String sheetId, SettingStore store,
            ObjectResponse<List<String>> response) {
        response.send(store.descendants(sheetId.trim()));
    }
}
