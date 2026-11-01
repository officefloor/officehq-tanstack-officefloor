package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the sheet's id then those of all its descendants (the sheets below it by parent). */
public class RespondWithFamily {
    public void respond(@PathVariable("sheet") String sheet, Settings settings,
            ObjectResponse<List<String>> response) {
        response.send(settings.family(sheet));
    }
}
