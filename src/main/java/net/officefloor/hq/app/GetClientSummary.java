package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/clients/summary?clientId=&lt;id&gt; — at-a-glance counts for one client: how many projects
 * it owns and how many contacts it keeps. Scoped to a client (the client detail page shows ITS
 * figures), so the client id arrives as a query parameter. A small cross-entity aggregate read, so
 * it injects both repositories and lets the database do the counting. Wired by
 * officefloor/rest/api/clients/summary.GET.yml.
 */
public class GetClientSummary {

    public void service(@RequestParam("clientId") String clientId,
            ProjectRepository projects, ContactRepository contacts,
            ObjectResponse<ClientSummaryView> response) {
        Long id = Long.valueOf(clientId);
        response.send(new ClientSummaryView(
                projects.countByClientId(id), contacts.countByClientId(id)));
    }
}
