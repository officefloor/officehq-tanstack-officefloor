package net.officefloor.hq.app;

import java.util.regex.Pattern;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients — create a client from {name, email} and return the saved row (with its id).
 * Wired by {@code officefloor/rest/api/clients.POST.yml}.
 *
 * <p>Every client must have a proper email address: an empty or malformed address is rejected with
 * 400 so the row is never persisted (the front-end enforces the same rule, and the DB carries a
 * CHECK constraint as the last line of defence).
 */
public class ClientsPost {

    // Mirror of the front-end EMAIL_PATTERN: one @, non-empty local/domain parts, dotted domain.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody NewClient body, ClientRepository clients,
            ObjectResponse<Client> response) {
        String email = body.getEmail();
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        Client client = new Client();
        client.setName(body.getName());
        client.setEmail(email.trim());
        response.send(clients.save(client));
    }
}
