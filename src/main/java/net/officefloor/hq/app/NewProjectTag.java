package net.officefloor.hq.app;

/**
 * The request body for attaching a label to a project: which tag to add (the project id comes from
 * the path). Bound from the POST JSON body via {@code @RequestBody}.
 */
public class NewProjectTag {

    private Long tagId;

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
