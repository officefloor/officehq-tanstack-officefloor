package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the sheet's claim templates, in the order first saved. */
public class RespondWithTemplates {
    public void respond(@PathVariable("sheet") String sheet, ClaimTemplates templates,
            ObjectResponse<List<ClaimTemplate>> response) {
        response.send(templates.list(sheet));
    }
}
