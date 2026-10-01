package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/notes?targetType=&lt;type&gt;&amp;targetId=&lt;id&gt; — the notes written against one
 * target, newest first. Scoped to a target (the detail page lists ITS notes), so the type and id
 * arrive as query parameters. Wired by officefloor/rest/api/notes.GET.yml.
 */
public class ListNotes {

    public void service(@RequestParam("targetType") String targetType,
            @RequestParam("targetId") String targetId, NoteRepository notes,
            ObjectResponse<List<NoteView>> response) {
        List<NoteView> view = notes
                .findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(targetType,
                        Long.valueOf(targetId))
                .stream().map(NoteView::of).toList();
        response.send(view);
    }
}
