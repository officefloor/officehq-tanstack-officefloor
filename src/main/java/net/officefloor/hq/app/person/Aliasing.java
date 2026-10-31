package net.officefloor.hq.app.person;

/** An alias just given to a person (as kept: trimmed), and by whom. */
public record Aliasing(Person person, String alias, String user) {
}
