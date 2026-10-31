package net.officefloor.hq.app.consolidated;

import java.util.List;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithDescendants {
    public void respond(@PathVariable("sheetId") String sheetId, Settings settings,
            ObjectResponse<List<String>> response) {
        response.send(settings.descendants(sheetId));
    }
}
