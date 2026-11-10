package net.officefloor.hq.app.delegation;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithDelegations {
    public void respond(@Val Sheet sheet, DelegationRepository repository,
            ObjectResponse<DelegationsResponse> response) {
        response.send(new DelegationsResponse(
                repository.findAll(sheet.id()).stream().map(DelegationResponse::from).toList()));
    }
}
