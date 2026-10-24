package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's rates; as of a date (asOf), as they stood at the end of it. */
public class RespondWithRates {
    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ObjectResponse<List<RateResponse>> response) throws InvalidTodayException {
        response.send(asOf.rates(sheet.getId(), AsOf.dateOf(asOfParam)).stream().map(RateResponse::from).toList());
    }
}
