package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId, PeriodStore store,
            ObjectResponse<List<PeriodResponse>> response) {
        response.send(store.list(sheetId.trim()));
    }
}
