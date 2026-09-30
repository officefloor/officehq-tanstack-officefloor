package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/clients} — list clients, oldest first. Wired by
 * {@code officefloor/rest/api/clients.GET.yml}.
 *
 * <p>An optional {@code q} query parameter narrows the list to clients whose name contains it
 * (case-insensitive) — this backs the name search box on the clients page. When {@code q} is
 * absent or blank, every client is returned.
 */
public class ClientsGet {

    public void service(@HttpQueryParameter("q") String q, ClientRepository repository,
            ObjectResponse<List<Client>> response) {
        String term = q == null ? "" : q.trim();
        if (term.isEmpty()) {
            response.send(repository.findAllByOrderByIdAsc());
        } else {
            response.send(repository.findByNameContainingIgnoreCaseOrderByIdAsc(term));
        }
    }
}
