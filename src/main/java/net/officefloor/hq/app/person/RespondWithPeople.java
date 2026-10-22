package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's people, in the order they were added (as they stood at the end of the as-of date, when one is given). */
public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PersonRepository repository,
            ObjectResponse<List<PersonResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        List<Person> people = end != null ? repository.listAsOf(id, end) : repository.list(id);
        response.send(people.stream().map(PersonResponse::from).toList());
    }
}
