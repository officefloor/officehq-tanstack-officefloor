package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithDescendants {
    // The ids of the sheet's descendants (see Settings#descendants); the sheet itself is not among them.
    public void respond(@PathVariable("sheetId") String sheetId, Settings settings, ObjectResponse<List<String>> response) {
        response.send(settings.descendants(sheetId.trim()));
    }
}
