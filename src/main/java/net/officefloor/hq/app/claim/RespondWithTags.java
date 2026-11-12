package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.TreeSet;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Every tag ever carried by a claim on the sheet (up to the end of the as-of date when given), sorted. */
public class RespondWithTags {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, ClaimStore store,
            ObjectResponse<List<String>> response) {
        TreeSet<String> tags = new TreeSet<>();
        for (String stored : store.tagsEverUsed(sheetId.trim(), History.date(asOf))) {
            tags.addAll(Tags.list(stored));
        }
        response.send(List.copyOf(tags));
    }
}
