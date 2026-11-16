package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithCrewMemberCreated {
    public void respond(@Val CrewMemberValues values, ObjectResponse<ResponseEntity<CrewMember>> response) {
        response.send(new ResponseEntity<>(new CrewMember(values.personId(), values.name()), HttpStatus.CREATED));
    }
}
