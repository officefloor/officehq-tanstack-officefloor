package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithPlanCreated {
    public void respond(@Val Plan plan, ObjectResponse<ResponseEntity<PlanResponse>> response) {
        response.send(new ResponseEntity<>(PlanResponse.from(plan), HttpStatus.CREATED));
    }
}
