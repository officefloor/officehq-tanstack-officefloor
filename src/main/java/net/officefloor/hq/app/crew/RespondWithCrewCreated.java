package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithCrewCreated {
    public void respond(@Val CrewValues values, ObjectResponse<ResponseEntity<Crew>> response) {
        response.send(new ResponseEntity<>(new Crew(values.name(), values.leadId(), values.lead(), List.of()),
                HttpStatus.CREATED));
    }
}
