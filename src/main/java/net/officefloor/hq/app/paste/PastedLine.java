package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.Person;

/**
 * One pasted line: the name it typed, its claim as the claim form validates it, (once found) its person and the id of
 * the DRAFT claim of theirs it updates (null: it adds a claim).
 */
public record PastedLine(String name, ClaimInput input, Person person, Long draft) {

    /** A line not yet found on the sheet. */
    public PastedLine(String name, ClaimInput input) {
        this(name, input, null, null);
    }
}
