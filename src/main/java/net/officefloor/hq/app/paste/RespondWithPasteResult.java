package net.officefloor.hq.app.paste;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPasteResult {
    public void respond(@Val PasteResult result, ObjectResponse<PasteResult> response) {
        response.send(result);
    }
}
