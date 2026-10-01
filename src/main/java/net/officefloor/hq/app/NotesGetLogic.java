package net.officefloor.hq.app;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/notes?targetType=<type>&targetId=<id>} — list the notes written against one target,
 * newest first, so a detail page shows its own notes with the latest on top. Wired by
 * {@code officefloor/rest/api/notes.GET.yml}.
 */
public class NotesGetLogic {

    public void service(@RequestParam("targetType") String targetType,
            @RequestParam("targetId") Long targetId, NoteRepository notes,
            ObjectResponse<List<Note>> response) {
        response.send(notes.findByTargetTypeAndTargetIdOrderByAtDesc(targetType, targetId));
    }
}
