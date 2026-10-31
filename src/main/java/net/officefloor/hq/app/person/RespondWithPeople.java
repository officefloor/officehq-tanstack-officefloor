package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, People people, AsOf asOfSheet,
            ObjectResponse<List<PersonResponse>> response) throws InvalidSheetRequestException {
        response.send(asOfSheet.view(sheetId, asOf,
                () -> people.onSheet(sheetId).stream().map(PersonResponse::from).toList()));
    }
}
