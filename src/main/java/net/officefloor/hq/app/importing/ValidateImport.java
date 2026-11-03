package net.officefloor.hq.app.importing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.NewPerson;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the people import box: each non-blank line is name,grade,terms,contact (the last two may be absent
 *  or blank), bound by the person form's rules. A name or contact given twice in the import (ignoring case)
 *  is refused, as is an import with no lines. */
public class ValidateImport {
    public void validate(@RequestBody ImportRequest request, Out<ImportInput> input) throws BadRequestException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PersonRequest> people = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        int n = 0;
        for (String raw : text.split("\r?\n", -1)) {
            n++;
            if (raw.isBlank()) {
                continue;
            }
            String[] f = raw.split(",", -1);
            try {
                if (f.length > 4) {
                    throw new BadRequestException("give name,grade,terms,contact");
                }
                PersonRequest person = NewPerson.check(new PersonRequest(f[0], f.length > 1 ? f[1] : "", null, "",
                        f.length > 2 ? f[2] : "", f.length > 3 ? f[3] : ""));
                if (!names.add(person.name().toLowerCase(Locale.ROOT))) {
                    throw new BadRequestException("name " + person.name() + " is given twice in the import");
                }
                if (!person.contact().isEmpty() && !contacts.add(person.contact())) {
                    throw new BadRequestException("contact " + person.contact() + " is given twice in the import");
                }
                people.add(person);
            } catch (BadRequestException e) {
                throw new BadRequestException("line " + n + ": " + e.getMessage());
            }
        }
        if (people.isEmpty()) {
            throw new BadRequestException("import at least one line");
        }
        input.set(new ImportInput(people));
    }
}
