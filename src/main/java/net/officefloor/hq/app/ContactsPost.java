package net.officefloor.hq.app;

import java.util.regex.Pattern;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/contacts — create a contact from {name, email, role, clientId} and return the saved row
 * (with its id). Wired by {@code officefloor/rest/api/contacts.POST.yml}. A contact must have a
 * name, a well-formed email, a role and belong to an existing client; anything missing or malformed
 * is rejected with 400 so the row is never persisted (the front-end enforces the same rules).
 */
public class ContactsPost {

    // Mirror of the front-end EMAIL_PATTERN: one @, non-empty local/domain parts, dotted domain.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody NewContact body, ContactRepository contacts,
            ClientRepository clients, ObjectResponse<Contact> response) {
        String name = body.getName();
        String email = body.getEmail();
        String role = body.getRole();
        Long clientId = body.getClientId();
        if (name == null || name.trim().isEmpty()
                || email == null || !EMAIL.matcher(email.trim()).matches()
                || role == null || role.trim().isEmpty()
                || clientId == null || !clients.existsById(clientId)) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        Contact contact = new Contact();
        contact.setName(name.trim());
        contact.setEmail(email.trim());
        contact.setRole(role.trim());
        contact.setClientId(clientId);
        response.send(contacts.save(contact));
    }
}
