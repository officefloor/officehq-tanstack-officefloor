package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public class SavePersonCreated {
    public void save(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, People people,
            ObjectResponse<ResponseEntity<PersonResponse>> response) {
        Person person = people.add(sheetId, request.name(), request.grade());
        response.send(new ResponseEntity<>(PersonResponse.from(person), HttpStatus.CREATED));
    }
}
