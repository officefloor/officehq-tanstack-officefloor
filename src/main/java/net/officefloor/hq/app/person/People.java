package net.officefloor.hq.app.person;

import java.util.List;

/** A sheet's people, in the order they were added, and every patron any of them has named, and the ids of
 *  the people the signed-in user watches, and every alias any of them has, and the ids of the people whose name
 *  or an alias is already a name or alias on another sheet of the sheet's tree (read as the store holds it now). */
public record People(List<Person> people, List<Patron> patrons, List<Long> watched, List<Alias> aliases,
        List<Long> clashing) {

    public People withClashing(List<Long> clashing) {
        return new People(people, patrons, watched, aliases, clashing);
    }
}
