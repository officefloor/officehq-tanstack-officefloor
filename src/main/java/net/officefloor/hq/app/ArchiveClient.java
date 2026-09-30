package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/clients/archive}: the id of the client to tuck away. Bound from
 * the JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class ArchiveClient {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
