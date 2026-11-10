package net.officefloor.hq.app.person;

/**
 * A person as the screen sends it, to add or to edit: a name, a grade (A, B or C) and the date the
 * grade is from (yyyy-MM-dd; blank for the sheet's today). An edit also names the version of the
 * person it was made from.
 */
public record PersonRequest(String name, String grade, Long version, String gradeFrom) {
    /** A person's name as the form takes it: trimmed, required, at most 200 characters. */
    public static String checkName(String raw) throws InvalidPersonException {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) throw new InvalidPersonException("A name is required");
        if (name.length() > 200) throw new InvalidPersonException("A name is at most 200 characters");
        return name;
    }
}
