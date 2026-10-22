package net.officefloor.hq.app.patron;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Every person's patrons on the sheet; only those named by the as-of date, when one is given. */
public class RespondWithPatrons {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PatronRepository repository,
            ObjectResponse<List<PatronResponse>> response) {
        response.send(repository.list(sheetId.trim(), AsOf.parse(asOf)).stream().map(PatronResponse::from).toList());
    }
}
