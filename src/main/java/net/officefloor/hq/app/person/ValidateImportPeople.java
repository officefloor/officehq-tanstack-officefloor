package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the import into lines (blank lines skipped) and checks each by the person form's rules; the
 * first line that fails refuses the whole import. An import with no lines is refused too.
 */
public class ValidateImportPeople {
    public void validate(@RequestBody ImportPeopleRequest request, Out<ImportPeopleLines> linesOut)
            throws InvalidPersonException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PersonRequest> lines = new ArrayList<>();
        int number = 0;
        for (String raw : text.split("\\r?\\n|\\r")) {
            number++;
            if (raw.isBlank()) continue;
            String[] fields = raw.split(",", 4);
            if (fields.length < 2) throw new InvalidPersonException("Line " + number + ": name,grade,terms,contact");
            String terms = fields.length > 2 ? fields[2] : "";
            String contact = fields.length > 3 ? fields[3] : "";
            try {
                lines.add(PersonRequest.checked(new PersonRequest(fields[0], fields[1], null, "", terms, contact, null)));
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + number + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) throw new InvalidPersonException("Paste at least one line of name,grade,terms,contact");
        linesOut.set(new ImportPeopleLines(lines));
    }
}
