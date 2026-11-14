package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListPeople {
    public void list(@PathVariable("sheetId") String sheetId, PersonRepository people,
            ObjectResponse<List<Person>> response) {
        response.send(people.list(sheetId.trim()));
    }
}
