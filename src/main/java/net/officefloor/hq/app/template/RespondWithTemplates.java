package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's claim templates in the order first saved. */
public class RespondWithTemplates {
    public void respond(@PathVariable("sheetId") String sheetId, TemplateRepository templates, ObjectResponse<List<TemplateResponse>> response) {
        response.send(templates.list(sheetId.trim()).stream().map(TemplateResponse::from).toList());
    }
}
