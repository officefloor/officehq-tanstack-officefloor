package net.officefloor.hq.app.plan;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every plan on the sheet, in the order added. */
public class RespondWithPlans {
    public void respond(@PathVariable("sheet") String sheet, Plans plans, ObjectResponse<List<Plan>> response) {
        response.send(plans.list(sheet));
    }
}
