package net.officefloor.hq.app.crew;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's crews; a read made as of a date (see {@link AsOf}) answers with them as they stood at the end of it. */
public class RespondWithCrews {
    public void respond(@Val Sheet sheet, CrewRepository repository, HttpServletRequest request,
            ObjectResponse<CrewsResponse> response) {
        LocalDate asOf = AsOf.of(request);
        List<CrewMember> members = repository.members(sheet.id(), asOf);
        response.send(new CrewsResponse(repository.findAll(sheet.id(), asOf).stream()
                .map(c -> CrewResponse.from(c, members.stream().filter(m -> m.crew().equals(c.name())).toList()))
                .toList()));
    }
}
