package net.officefloor.hq.app.delegation;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every delegation made on the sheet, oldest first. */
public class RespondWithDelegations {
    public void respond(@PathVariable("sheet") String sheet, Delegations delegations,
            ObjectResponse<List<Delegation>> response) {
        response.send(delegations.list(sheet));
    }
}
