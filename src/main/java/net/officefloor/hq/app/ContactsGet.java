package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/contacts} — list every contact, oldest first. The UI scopes them to one client.
 * Wired by {@code officefloor/rest/api/contacts.GET.yml}.
 */
public class ContactsGet {

    public void service(ContactRepository contacts, ObjectResponse<List<Contact>> response) {
        response.send(contacts.findAllByOrderByIdAsc());
    }
}
