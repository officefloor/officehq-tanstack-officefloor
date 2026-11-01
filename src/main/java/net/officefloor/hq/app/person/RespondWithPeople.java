package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with everyone on the sheet, in the order they were added. */
public class RespondWithPeople {
    public void respond(@PathVariable("sheet") String sheet, People people, ObjectResponse<List<Person>> response) {
        response.send(people.list(sheet));
    }
}
