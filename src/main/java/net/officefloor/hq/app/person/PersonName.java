package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;

/** The rule for a person's name, shared by every form that names a person. */
public final class PersonName {

    private PersonName() {
    }

    /** A person's name, trimmed: required, at most 200 characters. */
    public static String of(String typed) throws BadRequestException {
        String name = typed == null ? "" : typed.trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Enter a name");
        }
        if (name.length() > 200) {
            throw new BadRequestException("A name is at most 200 characters");
        }
        return name;
    }
}
