package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the grade's rate on the sheet, replacing any rate it had. */
public class ApplyRate {
    public void apply(@PathVariable("sheetId") String sheetId, @Val RateInput input, RateRepository repository,
            Out<Rate> rateOut) {
        String id = sheetId.trim();
        Rate rate = repository.findBySheetIdAndGrade(id, input.grade()).orElseGet(() -> new Rate(id, input.grade()));
        rate.setRate(input.rate());
        rateOut.set(rate);
    }
}
