package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/projects/{projectId}/tags — attach a label to a project (from the body's {@code tagId})
 * and return the project's updated label set. Wired by
 * {@code officefloor/rest/api/projects/{projectId}/tags.POST.yml}. An unknown project or tag is
 * rejected with 404, a missing tagId with 400. Attaching a label already present is a no-op (the
 * pairing is the primary key), and the current set is returned either way.
 */
public class ProjectTagAdd {

    public void service(@HttpPathParameter("projectId") String projectId,
            @RequestBody NewProjectTag body, ProjectRepository projects, TagRepository tags,
            ProjectTagRepository projectTags, ObjectResponse<List<Tag>> response) {
        Long id = Long.valueOf(projectId);
        Long tagId = body.getTagId();
        if (tagId == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        projects.findById(id).orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        tags.findById(tagId).orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        projectTags.save(new ProjectTag(id, tagId));
        response.send(tags.findByProjectId(id));
    }
}
