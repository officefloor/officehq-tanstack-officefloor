package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/clients/archive} — tuck a client away instead of deleting it: flag it archived so
 * it drops off the default client list and search while the row is kept. Wired by
 * {@code officefloor/rest/api/clients/archive.POST.yml}. The client must exist; the archiving is
 * audited through {@link Audit} so it can be checked back later.
 */
public class ClientsArchive {

    public void service(@RequestBody ArchiveClient body, ClientRepository clients, Audit audit,
            ObjectResponse<Client> response) {
        Long id = body.getId();
        Client client = id == null ? null : clients.findById(id).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("no such client");
        }
        client.setArchived(true);
        Client saved = clients.save(client);
        audit.record("CLIENT_ARCHIVED id=" + id);
        response.send(saved);
    }
}
