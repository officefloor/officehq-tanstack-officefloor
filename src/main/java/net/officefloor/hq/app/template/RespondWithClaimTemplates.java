package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithClaimTemplates {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimTemplates templates,
            ObjectResponse<List<ClaimTemplateResponse>> response) {
        response.send(templates.onSheet(sheetId).stream().map(ClaimTemplateResponse::from).toList());
    }
}
