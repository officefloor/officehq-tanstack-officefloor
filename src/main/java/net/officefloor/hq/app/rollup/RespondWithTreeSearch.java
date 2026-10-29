package net.officefloor.hq.app.rollup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithTreeSearch {
    // Each name with its grade in force on its sheet's today. This sheet's matches first in list order, then each other sheet of its tree by id; due-totals as the screen shows them.
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "text", required = false) String text, People people, Rollup rollup, Sheets sheetStore,
            ObjectResponse<List<String>> response) {
        String sheet = sheetId.trim();
        String key = text == null ? "" : text.toLowerCase(Locale.ROOT);
        Set<String> others = new TreeSet<>(people.tree(sheet));
        others.remove(sheet);
        List<String> sheets = new ArrayList<>(List.of(sheet));
        sheets.addAll(others);
        List<String> lines = new ArrayList<>();
        for (String at : sheets) {
            Figures figures = rollup.figures(at);
            LocalDate today = sheetStore.today(at);
            for (Person person : people.list(at)) {
                if (matches(person, key)) {
                    Figures.PersonFigures f = figures.people().get(person.id());
                    lines.add(at + " " + person.name() + " (" + person.gradeOn(today) + ") " + (f == null ? "n/a" : f.dueTotal()));
                }
            }
        }
        response.send(lines);
    }

    private static boolean matches(Person person, String key) {
        if (person.name().toLowerCase(Locale.ROOT).contains(key)) {
            return true;
        }
        return person.aliases().stream().anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(key));
    }
}
