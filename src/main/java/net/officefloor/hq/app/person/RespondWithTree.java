package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The other sheets of the sheet's tree (see PersonRepository.tree), sorted by sheet id. */
public class RespondWithTree {
    public void respond(@PathVariable("sheetId") String sheetId, PersonRepository repository,
            ObjectResponse<List<String>> response) {
        response.send(repository.tree(sheetId.trim()).stream().sorted().toList());
    }
}
