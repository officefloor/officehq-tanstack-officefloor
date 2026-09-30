package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients/{clientId}/contacts — every contact belonging to one client, oldest id first.
 * Wired by {@code officefloor/rest/api/clients/{clientId}/contacts.GET.yml}.
 */
public class ClientContactsGet {

    public void service(@HttpPathParameter("clientId") String clientId,
            ContactRepository contacts, ObjectResponse<List<Contact>> response) {
        response.send(contacts.findByClientIdOrderByIdAsc(Long.valueOf(clientId)));
    }
}
