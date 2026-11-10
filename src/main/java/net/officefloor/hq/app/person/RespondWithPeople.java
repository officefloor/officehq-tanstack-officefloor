package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Set;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's people; read as of a date, those added by then, each as they stood then. Whether each
 * clashes is read from the sheet's tree as it is now.
 */
public class RespondWithPeople {
    public void respond(@Val Sheet sheet, PersonRepository repository, SheetRepository sheets, HttpServletRequest request,
            ObjectResponse<PeopleResponse> response) {
        LocalDate asOf = AsOf.of(request);
        Set<String> tree = repository.namesIn(sheets.tree(sheet.id()));
        response.send(new PeopleResponse(repository.findAll(sheet.id(), asOf).stream()
                .filter((p) -> asOf == null || p.addedBy(asOf))
                .map((p) -> asOf == null ? p : repository.asOf(p, asOf))
                .map((p) -> PersonResponse.from(p, sheet.today(), PersonRepository.clashes(p, tree))).toList()));
    }
}
