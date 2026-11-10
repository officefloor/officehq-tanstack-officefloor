package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithVoidReasons {
    public void respond(@Val Sheet sheet, VoidReasonRepository repository, ObjectResponse<VoidReasonsResponse> response) {
        response.send(new VoidReasonsResponse(
                repository.findAll(sheet.id()).stream().map(VoidReasonResponse::from).toList()));
    }
}
