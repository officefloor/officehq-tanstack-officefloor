package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId, People people, Sheets sheets,
            ObjectResponse<List<PersonResponse>> response) {
        LocalDate today = sheets.today(sheetId.trim());
        response.send(people.list(sheetId.trim()).stream().map(p -> PersonResponse.from(p, today)).toList());
    }
}
