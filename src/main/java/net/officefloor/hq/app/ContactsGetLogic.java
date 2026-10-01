package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/contacts} — list every contact (each carrying its {@code clientId}), so the
 * client's page can filter to its own. Wired by {@code officefloor/rest/api/contacts.GET.yml}.
 * Mirrors the projects list: one shared collection, the client context is a filter on it.
 */
public class ContactsGetLogic {

    public void service(ContactRepository contacts, ObjectResponse<List<Contact>> response) {
        response.send(contacts.findAll());
    }
}
