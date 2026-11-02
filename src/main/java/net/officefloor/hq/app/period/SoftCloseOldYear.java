package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Soft-closes, in order of their from-dates, every period of the rolled-over year that is still open (neither closed
 * nor soft-closed), each as a soft close does and audited after the rollover.
 */
public class SoftCloseOldYear {
    public void softClose(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val RolloverYear year, Periods periods, PeriodCloser closer) {
        List<Period> open = periods.list(sheet).stream()
                .filter((p) -> !p.fixed() && !LocalDate.parse(p.from()).isBefore(year.from())
                        && !LocalDate.parse(p.from()).isAfter(year.to()))
                .sorted(Comparator.comparing(Period::from)).toList();
        for (Period period : open) {
            closer.close(sheet, user, period, false, true);
        }
    }
}
