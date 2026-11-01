package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with how many periods were generated and every period on the sheet. */
public class RespondWithGeneratedPeriods {
    public void respond(@PathVariable("sheet") String sheet, @Val Integer made, Periods periods,
            ObjectResponse<GeneratedPeriods> response) {
        response.send(new GeneratedPeriods(made, periods.list(sheet)));
    }
}
