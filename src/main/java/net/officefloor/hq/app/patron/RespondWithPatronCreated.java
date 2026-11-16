package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithPatronCreated {
    public void respond(@Val Person person, @Val PatronOf added, ObjectResponse<ResponseEntity<Patron>> response) {
        response.send(new ResponseEntity<>(new Patron(person.id(), added.patron().id()), HttpStatus.CREATED));
    }
}
