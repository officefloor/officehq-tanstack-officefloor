package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients — every client, in id order, as the list the front-end renders.
 * Wired by officefloor/rest/api/clients.GET.yml.
 */
public class ListClients {

    public void service(ClientRepository clients, ObjectResponse<List<ClientView>> response) {
        List<ClientView> view = clients.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(ClientView::of)
                .toList();
        response.send(view);
    }
}
