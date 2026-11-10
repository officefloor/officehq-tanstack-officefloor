package net.officefloor.hq.app.template;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithTemplates {
    public void respond(@Val Sheet sheet, TemplateRepository repository, ObjectResponse<TemplatesResponse> response) {
        response.send(new TemplatesResponse(
                repository.findAll(sheet.id()).stream().map(TemplateResponse::from).toList()));
    }
}
