package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's people, in the order they were added. */
public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId, PersonRepository repository,
            ObjectResponse<List<PersonResponse>> response) {
        response.send(repository.list(sheetId.trim()).stream().map(PersonResponse::from).toList());
    }
}
