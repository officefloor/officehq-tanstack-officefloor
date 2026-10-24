package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithDelegations {
    public void respond(@Val Sheet sheet, DelegationRepository repository,
            ObjectResponse<List<DelegationResponse>> response) {
        response.send(repository.findBySheetIdOrderByIdAsc(sheet.getId()).stream().map(DelegationResponse::from).toList());
    }
}
