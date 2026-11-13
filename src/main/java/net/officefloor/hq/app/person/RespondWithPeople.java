package net.officefloor.hq.app.person;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.alias.Alias;
import net.officefloor.hq.app.alias.AliasRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeople {
    public void respond(@Val Sheet sheet, @Val List<Person> people, AliasRepository aliases, NameTree tree,
            PersonRepository repository, ObjectResponse<List<PersonResponse>> response) {
        Set<String> taken = tree.taken(sheet.id());
        Map<Long, List<String>> byPerson = new HashMap<>();
        if (!taken.isEmpty()) {
            for (Alias a : aliases.findAll(sheet.id())) {
                byPerson.computeIfAbsent(a.personId(), (k) -> new java.util.ArrayList<>()).add(a.alias());
            }
        }
        Map<Long, String> origins = repository.origins(sheet.id());
        response.send(people.stream().map((p) -> PersonResponse.from(p,
                NameTree.clashing(taken, p.name(), byPerson.getOrDefault(p.id(), List.of())), origins.get(p.id())))
                .toList());
    }
}
