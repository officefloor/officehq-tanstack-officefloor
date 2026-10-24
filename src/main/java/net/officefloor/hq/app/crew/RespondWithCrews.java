package net.officefloor.hq.app.crew;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's crews, in the order they were added, each with its lead and its members in the order they were
 *  added. */
public class RespondWithCrews {
    public void respond(@Val Sheet sheet, CrewRepository crews, CrewMemberRepository members, PersonRepository people,
            ObjectResponse<List<CrewResponse>> response) {
        Map<Long, Person> byId = people.findBySheetIdIn(List.of(sheet.getId())).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        List<CrewResponse> out = new ArrayList<>();
        for (Crew crew : crews.findBySheetIdOrderByIdAsc(sheet.getId())) {
            Person lead = byId.get(crew.getLeadId());
            List<CrewResponse.Person> shown = new ArrayList<>();
            for (CrewMember m : members.findByCrewIdOrderByIdAsc(crew.getId())) {
                Person person = byId.get(m.getPersonId());
                if (person != null) shown.add(new CrewResponse.Person(person.getId(), person.getName()));
            }
            out.add(new CrewResponse(crew.getId(), crew.getName(),
                    lead == null ? null : new CrewResponse.Person(lead.getId(), lead.getName()), shown));
        }
        response.send(out);
    }
}
