package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the import into lines of name,grade,terms,contact (the last two may be absent or blank)
 *  and checks each by the person form's rules; blank lines are skipped. A name or contact repeated
 *  within the import, as they are compared, refuses it. */
public class ValidateImportPeople {
    public void validate(@RequestBody ImportPeopleRequest request, Out<ImportedPeople> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null ? null : request.user();
        String text = request == null || request.text() == null ? "" : request.text();
        List<PersonRequest> lines = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        int n = 0;
        for (String raw : text.split("\\r?\\n")) {
            n++;
            if (raw.isBlank()) continue;
            String[] f = raw.split(",", -1);
            if (f.length < 2 || f.length > 4) {
                throw new InvalidSheetRequestException("Line " + n + ": expected name,grade,terms,contact");
            }
            PersonRequest checked;
            try {
                checked = PersonRequest.checked(new PersonRequest(f[0], f[1].trim(), user, null, null,
                        f.length > 2 ? f[2] : "", f.length > 3 ? f[3] : ""));
            } catch (InvalidSheetRequestException e) {
                throw new InvalidSheetRequestException("Line " + n + ": " + e.getMessage());
            }
            if (!names.add(checked.name().toLowerCase(Locale.ROOT))) {
                throw new InvalidSheetRequestException("Line " + n + ": the name is repeated in the import: " + checked.name());
            }
            if (checked.contactKey() != null && !contacts.add(checked.contactKey())) {
                throw new InvalidSheetRequestException("Line " + n + ": the contact is repeated in the import: " + checked.contactKey());
            }
            lines.add(checked);
        }
        if (lines.isEmpty()) throw new InvalidSheetRequestException("Paste at least one line of name,grade,terms,contact");
        actor.set(new Actor(user));
        body.set(new ImportedPeople(lines, user));
    }
}
