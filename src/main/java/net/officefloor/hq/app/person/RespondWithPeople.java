package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId, PersonRepository repository,
            ObjectResponse<List<PersonResponse>> response) {
        response.send(repository.findBySheet(sheetId).stream().map(PersonResponse::from).toList());
    }
}
