package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every period on the sheet. */
public class RespondWithPeriods {
    public void respond(@PathVariable("sheet") String sheet, Periods periods, ObjectResponse<List<Period>> response) {
        response.send(periods.list(sheet));
    }
}
