package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId, PersonStore store,
            ObjectResponse<List<PersonResponse>> response) {
        response.send(store.list(sheetId.trim()).stream().map(PersonResponse::from).toList());
    }
}
