package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's people, in the order they were added. */
public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId, PersonRepository repository, SheetRepository sheets,
            ObjectResponse<PeopleResponse> response) {
        String sheet = sheetId.trim();
        LocalDate today = sheets.findById(sheet).map((s) -> s.getToday()).orElseGet(LocalDate::now);
        response.send(PeopleResponse.from(repository.findBySheetIdOrderByIdAsc(sheet), today));
    }
}
