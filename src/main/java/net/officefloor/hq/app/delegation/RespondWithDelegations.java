package net.officefloor.hq.app.delegation;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's delegations, in the order they were made. */
public class RespondWithDelegations {
    public void respond(@PathVariable("sheetId") String sheetId, DelegationRepository repository,
            ObjectResponse<List<DelegationResponse>> response) {
        response.send(repository.list(sheetId.trim()).stream().map(DelegationResponse::from).toList());
    }
}
