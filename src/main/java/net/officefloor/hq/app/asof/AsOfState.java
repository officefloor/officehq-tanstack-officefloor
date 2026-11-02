package net.officefloor.hq.app.asof;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/**
 * Puts a sheet, inside the caller's transaction, as it stood at the end of a date (see {@link SheetSnapshots#show}),
 * with the date for its today, as the as-of view shows it: its parents whose rates it takes (see Rates) are put as
 * they stood then too, bar a parent whose today was never set (it has no dated history, so it is taken as it stands
 * now) — unless the sheet itself had nothing then, when no parent is taken. So too the sheet it was cloned from, and
 * its parents and source in turn. The caller must roll back.
 */
@Service
public class AsOfState {

    private final SheetSnapshots snapshots;
    private final Settings settings;
    private final SheetClock clock;
    private final Rates rates;

    public AsOfState(SheetSnapshots snapshots, Settings settings, SheetClock clock, Rates rates) {
        this.snapshots = snapshots;
        this.settings = settings;
        this.clock = clock;
        this.rates = rates;
    }

    /** Puts the sheet as it stood at the end of the date; returns whether it had anything to show then. */
    public boolean show(String sheet, LocalDate date) {
        boolean had = snapshots.show(sheet, date);
        Set<String> shown = new HashSet<>(Set.of(sheet));
        for (String from = sheet; !from.isEmpty(); ) {
            String parent = settings.parent(from);
            for (int level = 1; level <= Rate.LEVELS && !parent.isEmpty() && shown.add(parent); level++) {
                if (clock.isSet(parent) || !had) {
                    snapshots.show(parent, date);
                }
                parent = settings.parent(parent);
            }
            from = rates.source(from);
            if (!from.isEmpty() && shown.add(from)) {
                if (clock.isSet(from) || !had) {
                    snapshots.show(from, date);
                }
            } else {
                from = "";
            }
        }
        return had;
    }
}
