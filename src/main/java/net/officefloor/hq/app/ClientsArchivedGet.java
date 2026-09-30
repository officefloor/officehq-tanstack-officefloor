package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/clients/archived} — list the archived (tucked-away) clients, oldest first. Wired by
 * {@code officefloor/rest/api/clients/archived.GET.yml}. Backs the clients-page "show archived"
 * panel, where each archived client is offered a restore back onto the default list. The default
 * {@code GET /api/clients} still returns only non-archived clients, so this is the only place the
 * hidden ones surface.
 */
public class ClientsArchivedGet {

    public void service(ClientRepository clients, ObjectResponse<List<Client>> response) {
        response.send(clients.findByArchivedTrueOrderByIdAsc());
    }
}
