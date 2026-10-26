package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithDelegations {
    public void respond(@PathVariable("sheetId") String sheetId, Delegations delegations,
            ObjectResponse<List<DelegationResponse>> response) {
        response.send(delegations.all(sheetId.trim()).stream().map(DelegationResponse::from).toList());
    }
}
