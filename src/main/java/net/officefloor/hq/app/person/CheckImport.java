package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the pasted lines (name,grade) and checks each with the form's rules: a name, a grade of A, B
 * or C, and a name not repeated in the paste (ignoring case; names are trimmed). A name already on
 * the sheet records that person's grade from the sheet's today; any other name adds a person. Blank lines are skipped. Any
 * bad line refuses the whole import.
 */
public class CheckImport {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void check(@PathVariable("sheetId") String sheetId, @RequestBody ImportRequest request,
            PersonRepository repository, SheetRepository sheets, Out<ImportedPeople> importedOut)
            throws InvalidImportException {
        String sheet = sheetId.trim();
        LocalDate today = sheets.findById(sheet).orElseThrow().getToday();
        Map<String, Person> existing = new HashMap<>();
        repository.findBySheetIdOrderByIdAsc(sheet).forEach((p) -> existing.putIfAbsent(key(p.getName()), p));
        Set<String> seen = new HashSet<>();
        String[] lines = request == null || request.text() == null ? new String[0] : request.text().split("\\R", -1);
        List<Person> people = new ArrayList<>();
        List<Person> updated = new ArrayList<>();
        List<String> grades = new ArrayList<>();
        List<Integer> errors = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }
            int comma = line.indexOf(',');
            String name = (comma < 0 ? line : line.substring(0, comma)).trim();
            String grade = comma < 0 ? "" : line.substring(comma + 1).trim();
            if (name.isEmpty() || !GRADES.contains(grade) || !seen.add(key(name))) {
                errors.add(i + 1);
                continue;
            }
            Person person = existing.get(key(name));
            if (person != null) {
                updated.add(person);
            } else {
                person = new Person(sheet);
                person.setName(name);
            }
            people.add(person);
            grades.add(grade);
        }
        if (!errors.isEmpty()) {
            throw new InvalidImportException(errors);
        }
        // Only now set the grades, so a refused import leaves the sheet's people untouched.
        for (int i = 0; i < people.size(); i++) {
            people.get(i).recordGrade(grades.get(i), today);
        }
        importedOut.set(new ImportedPeople(people, updated));
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
