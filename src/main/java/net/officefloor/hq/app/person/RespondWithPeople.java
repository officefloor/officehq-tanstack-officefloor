package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's people; read as of a date, those added by then, each as they stood then. */
public class RespondWithPeople {
    public void respond(@Val Sheet sheet, PersonRepository repository, HttpServletRequest request,
            ObjectResponse<PeopleResponse> response) {
        LocalDate asOf = AsOf.of(request);
        response.send(new PeopleResponse(repository.findAll(sheet.id()).stream()
                .filter((p) -> asOf == null || p.addedBy(asOf))
                .map((p) -> asOf == null ? p : repository.asOf(p, asOf))
                .map((p) -> PersonResponse.from(p, sheet.today())).toList()));
    }
}
