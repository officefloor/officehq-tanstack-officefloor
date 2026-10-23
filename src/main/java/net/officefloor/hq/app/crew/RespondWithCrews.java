package net.officefloor.hq.app.crew;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's crews in the order they were added, each with its lead and its members named as they are now. */
public class RespondWithCrews {
    public void respond(@PathVariable("sheetId") String sheetId, CrewRepository repository, PersonRepository people,
            ObjectResponse<List<CrewResponse>> response) {
        String id = sheetId.trim();
        Map<Long, String> names = new HashMap<>();
        for (Person p : people.list(id)) {
            names.put(p.id(), p.name());
        }
        response.send(repository.list(id).stream().map(c -> new CrewResponse(c.id(), c.name(), c.leadId(),
                Objects.requireNonNullElse(names.get(c.leadId()), ""),
                repository.members(c).stream().filter(names::containsKey)
                        .map(p -> new CrewResponse.Member(p, names.get(p))).toList()))
                .toList());
    }
}
