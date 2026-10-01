package net.officefloor.hq.app;

import java.util.regex.Pattern;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients — create a client from the submitted name + email, returning the saved row
 * (with its generated id). Wired by officefloor/rest/api/clients.POST.yml.
 *
 * A client must have a proper email address: we reject a blank or malformed email before persisting
 * so a bad row can never be saved, mirroring the UI check in ClientForm.tsx (the DB CHECK constraint
 * in V2__client_email_format.sql is the final guard).
 */
public class CreateClient {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody ClientForm form, ClientRepository clients,
            ObjectResponse<ClientView> response) {
        String email = form.getEmail() == null ? "" : form.getEmail().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        Client saved = clients.save(new Client(form.getName(), email));
        response.send(ClientView.of(saved));
    }
}
