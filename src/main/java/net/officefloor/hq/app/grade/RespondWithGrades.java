package net.officefloor.hq.app.grade;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithGrades {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, GradeStore store,
            ObjectResponse<List<GradeResponse>> response) {
        LocalDate on = History.date(asOf);
        List<String> retired = on == null ? store.retired(sheetId.trim()) : store.retiredAsOf(sheetId.trim(), on);
        response.send(store.grades(sheetId.trim()).stream().map(g -> new GradeResponse(g, retired.contains(g))).toList());
    }
}
