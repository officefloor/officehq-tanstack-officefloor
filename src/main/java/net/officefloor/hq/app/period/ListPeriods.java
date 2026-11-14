package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListPeriods {
    public void list(@PathVariable("sheetId") String sheetId, PeriodRepository periods,
            ObjectResponse<List<Period>> response) {
        response.send(periods.list(sheetId.trim()));
    }
}
