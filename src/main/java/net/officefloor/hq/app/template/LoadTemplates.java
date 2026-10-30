package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's claim templates, by name. */
public class LoadTemplates {
    public void service(@PathVariable("sheetId") String sheetId, ClaimTemplates templates, ObjectResponse<List<TemplateResponse>> response) {
        response.send(templates.list(sheetId).stream().map(TemplateResponse::from).toList());
    }
}
