package net.officefloor.hq.app.access;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithDelegations {
    public void respond(@Val SheetResponse sheet, DelegationRepository repository,
            ObjectResponse<List<DelegationResponse>> response) {
        response.send(repository.list(sheet.id()).stream().map(DelegationResponse::from).toList());
    }
}
