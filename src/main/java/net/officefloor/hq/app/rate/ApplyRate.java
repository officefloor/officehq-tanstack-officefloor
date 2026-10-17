package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the grade's rate from the from-date on the sheet, replacing any rate it had for that from-date. */
public class ApplyRate {
    public void apply(@PathVariable("sheetId") String sheetId, @Val RateInput input, RateRepository repository,
            Out<Rate> rateOut) {
        String id = sheetId.trim();
        Rate rate = repository.findFor(id, input.grade(), input.from())
                .orElseGet(() -> new Rate(id, input.grade(), input.from()));
        rate.setRate(input.rate());
        rateOut.set(rate);
    }
}
