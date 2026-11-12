package net.officefloor.hq.app.crew;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's crews, in the order they were added, each with its lead and members. */
public class RespondWithCrews {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, CrewStore store,
            ObjectResponse<List<CrewResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate on = History.date(asOf);
        Map<Long, List<CrewStore.Member>> members = store.members(sheet, on);
        response.send(store.list(sheet, on).stream().map(c -> new CrewResponse(c.id(), c.name(), c.leadId(),
                c.leadName(), members.getOrDefault(c.id(), List.of()).stream()
                        .map(m -> new CrewResponse.MemberResponse(m.personId(), m.name())).toList()))
                .toList());
    }
}
