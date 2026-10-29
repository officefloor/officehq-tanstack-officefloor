package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class MovePerson {
    // Swaps the person with the one above (up) or below (down) them in the list; the top cannot move up nor the bottom down.
    public void move(@PathVariable("direction") String direction, @Val Person person, People people, Sheets sheets,
            Out<MovedPerson> movedOut, Out<PersonResponse> responseOut) throws PersonMoveRefusedException {
        List<Person> list = people.list(person.sheetId());
        int at = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id() == person.id()) {
                at = i;
            }
        }
        int to = switch (direction) {
            case "up" -> at - 1;
            case "down" -> at + 1;
            default -> throw new PersonMoveRefusedException("No direction " + direction + ".");
        };
        if (at < 0 || to < 0 || to >= list.size()) {
            throw new PersonMoveRefusedException(person.name() + " cannot move " + direction + ".");
        }
        people.swap(person.id(), list.get(to).id());
        movedOut.set(new MovedPerson(person, to + 1));
        responseOut.set(PersonResponse.from(person, sheets.today(person.sheetId())));
    }
}
