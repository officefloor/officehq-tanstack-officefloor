package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients — every client the user still works with, in id order, as the list the front-end
 * renders. Archived (tucked-away) clients are retained but excluded here, so they drop off both the
 * list and the search (which filters the same list) without being lost.
 * Wired by officefloor/rest/api/clients.GET.yml.
 */
public class ListClients {

    public void service(ClientRepository clients, ObjectResponse<List<ClientView>> response) {
        List<ClientView> view = clients.findAll().stream()
                .filter(client -> !client.isArchived())
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(ClientView::of)
                .toList();
        response.send(view);
    }
}
