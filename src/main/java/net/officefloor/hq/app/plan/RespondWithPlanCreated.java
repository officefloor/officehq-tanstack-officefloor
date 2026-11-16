package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithPlanCreated {
    public void respond(@Val Plan plan, ObjectResponse<ResponseEntity<Plan>> response) {
        response.send(new ResponseEntity<>(plan, HttpStatus.CREATED));
    }
}
