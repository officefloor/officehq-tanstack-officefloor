package net.officefloor.hq.app.person;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's people, in the order they were added. */
public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId, PersonRepository repository,
            ObjectResponse<PeopleResponse> response) {
        response.send(PeopleResponse.from(repository.findBySheetIdOrderByIdAsc(sheetId.trim())));
    }
}
