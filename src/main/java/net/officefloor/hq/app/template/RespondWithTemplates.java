package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's claim templates, by name. */
public class RespondWithTemplates {
    public void respond(@PathVariable("sheetId") String sheetId, TemplateRepository repository,
            ObjectResponse<List<TemplateResponse>> response) {
        response.send(repository.list(sheetId.trim()).stream().map(TemplateResponse::from).toList());
    }
}
