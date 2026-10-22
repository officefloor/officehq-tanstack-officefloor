package net.officefloor.hq.app.person;

import java.util.List;

/** A people import that passed the person form's rules: who is making it, and each line's person, in order. */
public record ImportInput(String user, List<ImportLine> lines) {
}
