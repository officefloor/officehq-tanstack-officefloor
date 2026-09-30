package net.officefloor.hq.app;

import java.util.regex.Pattern;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/{clientId} — correct a client's name and/or email from {name, email} and return
 * the saved row. Wired by {@code officefloor/rest/api/clients/{clientId}.POST.yml}.
 *
 * <p>Same rules as creating a client: a proper (dotted, single-@) email address is required, and no
 * two clients may share an email — but the client keeping its own address must be allowed, so the
 * uniqueness check excludes this client. A malformed address or one already used by ANOTHER client
 * is rejected with 400 (the front-end enforces the same rules and the DB carries a UNIQUE
 * constraint), and an unknown client id with 404. Editing is an audited side-effect: one
 * {@code CLIENT_UPDATED id=<id>} record is appended per save (CLAUDE.md — audited behaviour goes
 * through {@link Audit}).
 */
public class ClientUpdate {

    // Mirror of the front-end EMAIL_PATTERN: one @, non-empty local/domain parts, dotted domain.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@HttpPathParameter("clientId") String clientId, @RequestBody NewClient body,
            ClientRepository clients, Audit audit, ObjectResponse<Client> response) {
        Client client = clients.findById(Long.valueOf(clientId))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        String email = body.getEmail();
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        if (clients.existsByEmailAndIdNot(email.trim(), client.getId())) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        client.setName(body.getName());
        client.setEmail(email.trim());
        clients.save(client);
        audit.record("CLIENT_UPDATED id=" + client.getId());
        response.send(client);
    }
}
