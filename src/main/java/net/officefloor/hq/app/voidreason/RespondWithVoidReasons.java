package net.officefloor.hq.app.voidreason;

import java.util.List;
import java.util.Map;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's void reasons in the order they were added, each with how many VOID claims were voided with it. */
public class RespondWithVoidReasons {
    public void respond(@PathVariable("sheetId") String sheetId, VoidReasonRepository repository,
            ObjectResponse<List<VoidReasonResponse>> response) {
        String id = sheetId.trim();
        Map<String, Integer> voided = repository.voided(id);
        response.send(repository.list(id).stream()
                .map(r -> VoidReasonResponse.from(r, voided.getOrDefault(r.reason(), 0))).toList());
    }
}
