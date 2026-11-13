package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's claim templates as the screen reads them, by name. */
public class RespondWithTemplates {
    public void respond(@Val ClaimTemplates templates, ObjectResponse<List<TemplateResponse>> response) {
        response.send(templates.list().stream().map(TemplateResponse::from).toList());
    }
}
