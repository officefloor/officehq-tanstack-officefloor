package net.officefloor.hq.app.setting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's descendants: its children (the sheets naming it as their parent), their children, and so on, each
 *  once, nearest first. */
public class RespondWithDescendants {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository settings,
            ObjectResponse<List<String>> response) {
        String id = sheetId.trim();
        Set<String> seen = new LinkedHashSet<>();
        seen.add(id);
        Deque<String> next = new ArrayDeque<>(List.of(id));
        while (!next.isEmpty()) {
            for (String child : settings.children(next.removeFirst())) {
                if (seen.add(child)) {
                    next.addLast(child);
                }
            }
        }
        seen.remove(id);
        response.send(new ArrayList<>(seen));
    }
}
