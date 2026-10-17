package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the pasted lines (name,grade) and checks each with the form's rules: a name, a grade of A, B
 * or C, and a name not already on the sheet nor repeated in the paste (ignoring case; names are
 * trimmed). Blank lines are skipped. Any bad line refuses the whole import.
 */
public class CheckImport {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void check(@PathVariable("sheetId") String sheetId, @RequestBody ImportRequest request,
            PersonRepository repository, Out<ImportedPeople> importedOut) throws InvalidImportException {
        String sheet = sheetId.trim();
        Set<String> taken = new HashSet<>();
        repository.findBySheetIdOrderByIdAsc(sheet).forEach((p) -> taken.add(key(p.getName())));
        String[] lines = request == null || request.text() == null ? new String[0] : request.text().split("\\R", -1);
        List<Person> people = new ArrayList<>();
        List<Integer> errors = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }
            int comma = line.indexOf(',');
            String name = (comma < 0 ? line : line.substring(0, comma)).trim();
            String grade = comma < 0 ? "" : line.substring(comma + 1).trim();
            if (name.isEmpty() || !GRADES.contains(grade) || !taken.add(key(name))) {
                errors.add(i + 1);
                continue;
            }
            Person person = new Person(sheet);
            person.setName(name);
            person.setGrade(grade);
            people.add(person);
        }
        if (!errors.isEmpty()) {
            throw new InvalidImportException(errors);
        }
        importedOut.set(new ImportedPeople(people));
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
