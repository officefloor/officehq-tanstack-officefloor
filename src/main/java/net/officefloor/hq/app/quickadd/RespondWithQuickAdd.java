package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.People;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the sheet's people and claims. */
public class RespondWithQuickAdd {
    public void respond(@PathVariable("sheet") String sheet, People people, Claims claims,
            ObjectResponse<QuickAddResponse> response) {
        response.send(new QuickAddResponse(people.list(sheet), claims.list(sheet)));
    }
}
