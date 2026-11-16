package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The ids of the other sheets of the sheet's tree, sorted by id (see SettingRepository#tree). */
public class RespondWithTree {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository settings,
            ObjectResponse<List<String>> response) {
        response.send(settings.tree(sheetId.trim()).stream().sorted().toList());
    }
}
