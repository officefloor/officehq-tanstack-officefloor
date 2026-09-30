package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/clients/restore} — bring an archived client back: clear its archived flag so it
 * returns to the default client list and search. The inverse of {@link ClientsArchive}. Wired by
 * {@code officefloor/rest/api/clients/restore.POST.yml}. The client must exist; the restore is
 * audited through {@link Audit} so it can be checked back later.
 */
public class ClientsRestore {

    public void service(@RequestBody RestoreClient body, ClientRepository clients, Audit audit,
            ObjectResponse<Client> response) {
        Long id = body.getId();
        Client client = id == null ? null : clients.findById(id).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("no such client");
        }
        client.setArchived(false);
        Client saved = clients.save(client);
        audit.record("CLIENT_RESTORED id=" + id);
        response.send(saved);
    }
}
