package net.officefloor.hq.app.rollup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Snapshot;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithSnapshotCompare {
    // Per person in list order, "<name> <then> <now>", trimmed. A day's figure is the last SNAPSHOT written on leaving
    // it (empty when there is none); a blank from-date is the sheet's today, a blank to-date is the due-total now.
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to, People people, Rollup rollup, Sheets sheets,
            ObjectResponse<List<String>> response) {
        String sheet = sheetId.trim();
        LocalDate fromDay = blank(from) ? sheets.today(sheet) : LocalDate.parse(from.trim());
        LocalDate toDay = blank(to) ? null : LocalDate.parse(to.trim());
        Figures figures = toDay == null ? rollup.figures(sheet) : null;
        List<String> lines = new ArrayList<>();
        for (Person person : people.list(sheet)) {
            String then = snapshotOn(person, fromDay);
            String now;
            if (toDay != null) {
                now = snapshotOn(person, toDay);
            } else {
                Figures.PersonFigures f = figures.people().get(person.id());
                now = f == null ? "n/a" : f.dueTotal();
            }
            lines.add((person.name() + " " + then + " " + now).stripTrailing());
        }
        response.send(lines);
    }

    private static String snapshotOn(Person person, LocalDate day) {
        String found = "";
        for (Snapshot s : person.snapshots()) {
            if (s.day().equals(day)) {
                found = s.dueTotal();
            }
        }
        return found;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
