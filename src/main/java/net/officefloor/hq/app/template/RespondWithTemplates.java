package net.officefloor.hq.app.template;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithTemplates {
    public void respond(@Val Templates templates, ObjectResponse<Templates> response) {
        response.send(templates);
    }
}
