package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import net.officefloor.hq.app.sheet.AsOf;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's people, in the order they were added; read as of a date (see AsOf), as they stood then. */
public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, PersonRepository people,
            ObjectResponse<List<Person>> response) {
        java.time.LocalDate date = AsOf.parse(asOf);
        response.send(date == null ? people.list(sheetId.trim()) : people.listAsOf(sheetId.trim(), date));
    }
}
