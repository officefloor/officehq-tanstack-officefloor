package net.officefloor.hq.app.template;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithTemplate {
    public void respond(@Val ClaimTemplate template, ObjectResponse<ClaimTemplate> response) {
        response.send(template);
    }
}
