package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithTemplates {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimTemplates templates,
            ObjectResponse<List<TemplateResponse>> response) {
        response.send(templates.list(sheetId.trim()).stream().map(TemplateResponse::from).toList());
    }
}
