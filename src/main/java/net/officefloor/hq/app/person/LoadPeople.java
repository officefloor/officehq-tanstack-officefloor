package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's people; with an as-of date, as they stood at the end of it. */
public class LoadPeople {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, People people, AsOfView view,
            ObjectResponse<List<PersonResponse>> response) {
        LocalDate date = AsOfView.parse(asOf);
        response.send(date == null ? PersonResponse.from(people.list(sheetId)) : view.people(sheetId, date));
    }
}
