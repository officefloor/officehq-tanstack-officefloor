package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The import box's lines of name,grade,terms,contact (terms and contact may be absent or blank; blank lines are
 * skipped). Each line's fields are checked with the person form's rules; a name (ignoring case) or contact repeated
 * within the import refuses it. A refusal names the line. Publishes them parsed, in order, with the user making the
 * change.
 */
public class ValidateImportPeople {

    static final int MAX_LINES = 1000;

    public void validate(@RequestBody ImportPeopleRequest request, Out<ImportedPeople> imported,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String text = request == null || request.lines() == null ? "" : request.lines();
        List<PersonRequest> people = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        String[] typed = text.split("\\r?\\n");
        for (int i = 0; i < typed.length; i++) {
            if (typed[i].trim().isEmpty()) {
                continue;
            }
            String[] fields = typed[i].split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new BadRequestException("Line " + (i + 1) + ": enter name,grade,terms,contact");
            }
            try {
                PersonRequest person = ValidatePerson.parse(new PersonRequest(fields[0], fields[1], by.name(), null,
                        "", fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : ""), by);
                if (!names.add(person.name().toLowerCase(Locale.ROOT))) {
                    throw new BadRequestException(person.name() + " is repeated in the import");
                }
                if (person.contactOrNull() != null && !contacts.add(person.contact())) {
                    throw new BadRequestException("The contact " + person.contact() + " is repeated in the import");
                }
                people.add(person);
            } catch (BadRequestException e) {
                throw new BadRequestException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (people.isEmpty()) {
            throw new BadRequestException("Paste at least one line of name,grade,terms,contact");
        }
        if (people.size() > MAX_LINES) {
            throw new BadRequestException("Import at most " + MAX_LINES + " lines at once");
        }
        imported.set(new ImportedPeople(people));
        changedBy.set(by);
    }
}
