package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.jdbc.core.JdbcTemplate;

/** The sheet's rates, then its parents' (see {@link RateRepository#chain}). Viewed as of a date, the sheet's own are
 *  as they stood then; a view holds only the sheet's own rows, so its parents' are read as they stand. */
public class RespondWithRates {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history, JdbcTemplate jdbc,
            ObjectResponse<List<RateResponse>> response) {
        List<Rate> found = new ArrayList<>(history.view(sheet.id(), asOf.on(), view -> new RateRepository(view).list(sheet.id())));
        List<String> ancestry = new RateRepository(jdbc).ancestry(sheet.id());
        for (int level = 1; level < ancestry.size(); level++) {
            int at = level;
            new RateRepository(jdbc).list(ancestry.get(level)).forEach(r -> found.add(r.at(at)));
        }
        response.send(found.stream().map(RateResponse::from).toList());
    }
}
