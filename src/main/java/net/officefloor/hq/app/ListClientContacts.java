package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/clients/contacts?clientId=&lt;id&gt; — the contacts of one client, in id order. Scoped to
 * a client (the client detail page lists ITS contacts), so the client id arrives as a query
 * parameter. Wired by officefloor/rest/api/clients/contacts.GET.yml.
 */
public class ListClientContacts {

    public void service(@RequestParam("clientId") String clientId,
            ContactRepository contacts, ObjectResponse<List<ContactView>> response) {
        Long id = Long.valueOf(clientId);
        List<ContactView> view = contacts.findByClientIdOrderByIdAsc(id).stream()
                .map(ContactView::of)
                .toList();
        response.send(view);
    }
}
