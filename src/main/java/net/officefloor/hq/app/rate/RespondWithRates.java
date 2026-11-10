package net.officefloor.hq.app.rate;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's rates, by grade then from-date, then its parent sheets' by level; read as of a date, as they stood then. */
public class RespondWithRates {
    public void respond(@Val Sheet sheet, RateRepository repository, HttpServletRequest request,
            ObjectResponse<RatesResponse> response) {
        LocalDate asOf = AsOf.of(request);
        List<Rate> rates = asOf == null ? repository.findAll(sheet.id())
                : repository.findAsOf(sheet.id(), asOf).stream()
                        .sorted(Comparator.comparingInt(Rate::level).thenComparing(Rate::grade).thenComparing(Rate::from,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .toList();
        response.send(new RatesResponse(rates.stream().map(RateResponse::from).toList()));
    }
}
