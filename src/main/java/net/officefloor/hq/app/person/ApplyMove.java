package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Swaps the person with the one above or below them in the list; refuses moving the top one up or the bottom one
 *  down. */
public class ApplyMove {
    public void apply(@Val MoveInput input, @Val Person person, PersonRepository repository, Out<PersonMoved> moved)
            throws InvalidPersonException {
        List<Person> listed = repository.findListed(person.getSheetId());
        int at = 0;
        while (!listed.get(at).getId().equals(person.getId())) {
            at++;
        }
        int to = input.up() ? at - 1 : at + 1;
        if (to < 0 || to >= listed.size()) {
            throw new InvalidPersonException(input.up() ? "The top person cannot move up." : "The bottom person cannot move down.");
        }
        Person other = listed.get(to);
        long mine = person.getPlace();
        long theirs = other.getPlace();
        repository.placeAt(person.getId(), theirs);
        repository.placeAt(other.getId(), mine);
        moved.set(new PersonMoved(to + 1));
    }
}
