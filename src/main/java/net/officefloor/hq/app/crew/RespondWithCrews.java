package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every crew on the sheet. */
public class RespondWithCrews {
    public void respond(@PathVariable("sheet") String sheet, Crews crews, ObjectResponse<List<Crew>> response) {
        response.send(crews.list(sheet));
    }
}
