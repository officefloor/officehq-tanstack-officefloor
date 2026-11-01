package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every grade's rate on the sheet. */
public class RespondWithRates {
    public void respond(@PathVariable("sheet") String sheet, Rates rates, ObjectResponse<List<Rate>> response) {
        response.send(rates.list(sheet));
    }
}
