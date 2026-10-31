package net.officefloor.hq.app.template;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaimTemplate {
    public void respond(@Val ClaimTemplate template, ObjectResponse<ClaimTemplateResponse> response) {
        response.send(ClaimTemplateResponse.from(template));
    }
}
