package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/notes — write a note against a target from the submitted type, id and text, returning
 * the saved row (with its generated id and write time). Wired by officefloor/rest/api/notes.POST.yml.
 *
 * A note must name a target and have something to say: we reject a blank target type, a missing
 * target id, or blank text before persisting. The write time is stamped by the server (now), so a
 * freshly written note sorts above every seeded one in the newest-first list.
 */
public class CreateNote {

    public void service(@RequestBody NoteForm form, NoteRepository notes,
            ObjectResponse<NoteView> response) {
        String targetType = form.getTargetType() == null ? "" : form.getTargetType().trim();
        if (targetType.isEmpty()) {
            throw new IllegalArgumentException("A note target type is required");
        }
        Long targetId = form.getTargetId();
        if (targetId == null) {
            throw new IllegalArgumentException("A note target id is required");
        }
        String text = form.getText() == null ? "" : form.getText().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Note text is required");
        }
        Note saved = notes.save(new Note(targetType, targetId, text));
        response.send(NoteView.of(saved));
    }
}
