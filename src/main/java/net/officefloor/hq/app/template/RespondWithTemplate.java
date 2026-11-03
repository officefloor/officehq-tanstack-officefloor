package net.officefloor.hq.app.template;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithTemplate {
    public void respond(@Val Template template, ObjectResponse<Template> response) {
        response.send(template);
    }
}
