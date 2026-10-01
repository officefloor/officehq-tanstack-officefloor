package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients — create a client from the submitted name + email, returning the saved row
 * (with its generated id). Wired by officefloor/rest/api/clients.POST.yml.
 */
public class CreateClient {

    public void service(@RequestBody ClientForm form, ClientRepository clients,
            ObjectResponse<ClientView> response) {
        Client saved = clients.save(new Client(form.getName(), form.getEmail()));
        response.send(ClientView.of(saved));
    }
}
