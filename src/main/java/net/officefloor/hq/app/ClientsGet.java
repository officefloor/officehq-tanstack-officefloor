package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/clients} — list non-archived clients, oldest first. Wired by
 * {@code officefloor/rest/api/clients.GET.yml}. Archived clients are kept but tucked away, so they
 * drop off both this list and the search.
 *
 * <p>An optional {@code q} query parameter narrows the list to clients whose name contains it
 * (case-insensitive) — this backs the name search box on the clients page. When {@code q} is
 * absent or blank, every non-archived client is returned.
 */
public class ClientsGet {

    public void service(@HttpQueryParameter("q") String q, ClientRepository repository,
            ObjectResponse<List<Client>> response) {
        String term = q == null ? "" : q.trim();
        if (term.isEmpty()) {
            response.send(repository.findByArchivedFalseOrderByIdAsc());
        } else {
            response.send(repository.findByArchivedFalseAndNameContainingIgnoreCaseOrderByIdAsc(term));
        }
    }
}
