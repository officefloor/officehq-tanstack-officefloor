package net.officefloor.hq.app.snapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Responds with the comparison of two snapshot dates, per person in list order: {@code <name> <then> <now>}, where
 * {@code then} is their due-total in the SNAPSHOT written on leaving the from-date (empty when there is none) and
 * {@code now} likewise for the to-date, or their due-total now when the to-date is blank. Each line is trimmed.
 */
public class RespondWithSnapshotCompare {
    public void respond(@PathVariable("sheet") String sheet,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to, SnapshotLines lines, Rollup rollup,
            SheetClock clock, ObjectResponse<List<String>> response) {
        String fromDay = from == null || from.isBlank() ? clock.today(sheet).toString() : day(from);
        String toDay = to == null || to.isBlank() ? null : day(to);
        List<SnapshotLine> all = lines.list(sheet);
        List<String> result = new ArrayList<>();
        for (Map.Entry<Long, Rollup.Figures> entry : rollup.take(sheet).people().entrySet()) {
            long person = entry.getKey();
            String then = latest(all, person, fromDay);
            String now = toDay == null ? entry.getValue().dueTotal() : latest(all, person, toDay);
            result.add((entry.getValue().name() + " " + then + " " + now).trim());
        }
        response.send(result);
    }

    /** The day as yyyy-mm-dd, or the text as given when it is not a date (it then matches no snapshot). */
    private static String day(String text) {
        try {
            return LocalDate.parse(text.trim()).toString();
        } catch (RuntimeException e) {
            return text.trim();
        }
    }

    /** The person's due-total in the latest snapshot written on leaving the day, or empty when there is none. */
    private static String latest(List<SnapshotLine> all, long person, String day) {
        String found = "";
        for (SnapshotLine line : all) {
            if (line.person() == person && line.day().equals(day)) {
                found = line.dueTotal();
            }
        }
        return found;
    }
}
