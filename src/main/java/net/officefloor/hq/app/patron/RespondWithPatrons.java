package net.officefloor.hq.app.patron;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every patron named on the sheet. */
public class RespondWithPatrons {
    public void respond(@PathVariable("sheet") String sheet, Patrons patrons, ObjectResponse<List<Patron>> response) {
        response.send(patrons.list(sheet));
    }
}
