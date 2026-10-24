package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's claim templates, by name. */
public class RespondWithTemplates {
    public void respond(@Val Sheet sheet, ClaimTemplateRepository repository, ObjectResponse<List<TemplateResponse>> response) {
        response.send(repository.findBySheetIdOrderByNameAsc(sheet.getId()).stream().map(TemplateResponse::from).toList());
    }
}
