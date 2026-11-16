package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import net.officefloor.hq.app.sheet.AsOf;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's people, in list order (see PersonRepository#list); read as of a date (see AsOf), as they stood then. Each is marked
 *  with whether they clash (see PersonRepository#withClashing). */
public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, PersonRepository people,
            ObjectResponse<List<Person>> response) {
        java.time.LocalDate date = AsOf.parse(asOf);
        String sheet = sheetId.trim();
        response.send(people.withClashing(sheet, date == null ? people.list(sheet) : people.listAsOf(sheet, date)));
    }
}
