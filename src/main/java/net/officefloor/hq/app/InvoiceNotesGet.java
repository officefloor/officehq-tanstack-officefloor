package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/invoices/{id}/notes — every note written on one invoice, newest first. Wired by
 * {@code officefloor/rest/api/invoices/{id}/notes.GET.yml}. Notes are already a generic target
 * (target_type/target_id, Flyway V14); this reads the ones on an {@code "invoice"} target, the
 * same way {@link ProjectNotesGet} reads a project's.
 */
public class InvoiceNotesGet {

    public void service(@HttpPathParameter("id") String id, NoteRepository notes,
            ObjectResponse<List<Note>> response) {
        response.send(
                notes.findByTargetTypeAndTargetIdOrderByAtDescIdDesc("invoice", Long.valueOf(id)));
    }
}
