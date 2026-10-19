package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.Optional;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/** The sheet's period numbering from (yyyy-MM-dd; blank means off). While it is a date and the sheet's
 *  today is on or after it, a claim opened is numbered within its period (see the claim numbering). */
@Service
public class PeriodNumberingFrom {

    public static final String KEY = "period-numbering-from";

    private final SettingRepository settings;
    private final SheetClock clock;

    public PeriodNumberingFrom(SettingRepository settings, SheetClock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    /** Whether a claim opened now on the sheet is numbered within its period. */
    public boolean inForce(String sheetId) {
        Optional<LocalDate> from = settings.find(sheetId, KEY).map(LocalDate::parse);
        return from.isPresent() && !clock.today(sheetId).isBefore(from.get());
    }
}
