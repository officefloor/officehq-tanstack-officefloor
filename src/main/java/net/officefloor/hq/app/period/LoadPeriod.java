package net.officefloor.hq.app.period;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads a period of the sheet by id. */
public class LoadPeriod {
    public void load(@Val Sheet sheet, @PathVariable("periodId") String periodId, PeriodRepository repository,
            Out<Period> periodOut) throws PeriodNotFoundException {
        Long id;
        try {
            id = Long.valueOf(periodId.trim());
        } catch (NumberFormatException e) {
            throw new PeriodNotFoundException(periodId);
        }
        periodOut.set(repository.findByIdAndSheetId(id, sheet.getId())
                .orElseThrow(() -> new PeriodNotFoundException(periodId)));
    }
}
