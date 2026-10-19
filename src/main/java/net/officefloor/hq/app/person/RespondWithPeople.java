package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId, PersonRepository repository, SheetClock clock,
            ObjectResponse<List<PersonResponse>> response) {
        LocalDate today = clock.today(sheetId);
        response.send(repository.findBySheet(sheetId).stream().map(p -> PersonResponse.from(p, today)).toList());
    }
}
