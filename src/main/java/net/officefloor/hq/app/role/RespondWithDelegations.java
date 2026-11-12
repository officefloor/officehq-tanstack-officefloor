package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithDelegations {
    public void respond(@PathVariable("sheetId") String sheetId, DelegationStore store,
            ObjectResponse<List<DelegationResponse>> response) {
        response.send(store.list(sheetId.trim()).stream().map(DelegationResponse::of).toList());
    }
}
