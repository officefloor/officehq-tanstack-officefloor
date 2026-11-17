package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every approver on the sheet, by person id. */
public class RespondWithApprovers {
    public void respond(@PathVariable("sheetId") String sheetId, PersonRepository people,
            ObjectResponse<List<Approver>> response) {
        response.send(people.approvers(sheetId.trim()));
    }
}
