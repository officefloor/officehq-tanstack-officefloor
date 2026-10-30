package net.officefloor.hq.app.rollup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithTreePeople {
    // One row per person: sheet id, name, grade in force on its sheet's today, due-total as the screen shows it.
    // This sheet's people first in list order, then each other sheet of its tree by id.
    public void respond(@PathVariable("sheetId") String sheetId, People people, Rollup rollup, Sheets sheetStore,
            ObjectResponse<List<List<String>>> response) {
        String sheet = sheetId.trim();
        Set<String> others = new TreeSet<>(people.tree(sheet));
        others.remove(sheet);
        List<String> sheets = new ArrayList<>(List.of(sheet));
        sheets.addAll(others);
        List<List<String>> rows = new ArrayList<>();
        for (String at : sheets) {
            Figures figures = rollup.figures(at);
            LocalDate today = sheetStore.today(at);
            for (Person person : people.list(at)) {
                Figures.PersonFigures f = figures.people().get(person.id());
                rows.add(List.of(at, person.name(), String.valueOf(person.gradeOn(today)),
                        f == null ? "n/a" : f.dueTotal()));
            }
        }
        response.send(rows);
    }
}
