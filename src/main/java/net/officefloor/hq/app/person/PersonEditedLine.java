package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.server.http.ServerHttpConnection;

/** The PERSON_EDITED audit line, for every save that edits a person (kept apart: a function class has one method). */
public final class PersonEditedLine {

    private PersonEditedLine() {
    }

    /** The PERSON_EDITED line of a save; {@code fromStart} when it recorded terms from the beginning of time. */
    public static void record(Person loaded, PersonResponse edited, boolean fromStart, ServerHttpConnection connection,
            Audit audit) {
        List<String> fields = new ArrayList<>(List.of("name=" + edited.name(), "grade=" + edited.grade()));
        if (fromStart || !PersonResponse.termses(loaded).equals(edited.termses())) {
            fields.add("terms=" + (edited.terms() == null ? "" : edited.terms()));
        }
        if (!Objects.equals(loaded.contact(), edited.contact())) {
            fields.add("contact=" + (edited.contact() == null ? "" : edited.contact()));
        }
        audit.record(Audit.line("PERSON_EDITED", loaded.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
    }
}
