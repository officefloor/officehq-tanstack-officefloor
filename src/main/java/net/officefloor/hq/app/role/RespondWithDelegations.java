package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's delegations, in the order they were made. */
public class RespondWithDelegations {
    public void respond(@PathVariable("sheetId") String sheetId, DelegationRepository delegations,
            ObjectResponse<List<Delegation>> response) {
        response.send(delegations.list(sheetId.trim()));
    }
}
