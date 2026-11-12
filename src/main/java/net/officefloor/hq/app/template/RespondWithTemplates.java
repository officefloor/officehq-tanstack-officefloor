package net.officefloor.hq.app.template;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithTemplates {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, TemplateStore store,
            ObjectResponse<List<TemplateResponse>> response) {
        LocalDate on = History.date(asOf);
        response.send((on == null ? store.list(sheetId.trim()) : store.listAsOf(sheetId.trim(), on)).stream()
                .map(TemplateResponse::from).toList());
    }
}
