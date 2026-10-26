package net.officefloor.hq.app.person;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

    /** The names (or aliases) sorted as names are compared: ignoring case. */
    public static List<String> sorted(List<String> names) {
        return names.stream().sorted(Comparator.comparing((String n) -> n.toLowerCase(Locale.ROOT))
                .thenComparing(Comparator.naturalOrder())).toList();
    }
}
