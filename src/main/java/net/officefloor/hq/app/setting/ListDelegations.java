package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListDelegations {
    public void list(@PathVariable("sheetId") String sheetId, DelegationRepository delegations,
            ObjectResponse<List<Delegation>> response) {
        response.send(delegations.list(sheetId.trim()));
    }
}
