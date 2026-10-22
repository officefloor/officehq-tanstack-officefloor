package net.officefloor.hq.app.patron;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every person's patrons on the sheet. */
public class RespondWithPatrons {
    public void respond(@PathVariable("sheetId") String sheetId, PatronRepository repository,
            ObjectResponse<List<PatronResponse>> response) {
        response.send(repository.list(sheetId.trim()).stream().map(PatronResponse::from).toList());
    }
}
