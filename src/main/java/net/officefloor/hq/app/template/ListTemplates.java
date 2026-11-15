package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListTemplates {
    public void list(@PathVariable("sheetId") String sheetId, ClaimTemplateRepository templates,
            ObjectResponse<List<ClaimTemplate>> response) {
        response.send(templates.list(sheetId.trim()));
    }
}
