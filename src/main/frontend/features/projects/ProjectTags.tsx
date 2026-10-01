import { useState } from 'react';
import { useAddProjectTag, useAllTags, useProjectTags, useRemoveProjectTag } from './tags';

// A project's tags: the labels it carries (as removable chips) plus a picker to add another. Reads
// its own query keys (the project's tags, and the full tag catalogue) and never copies them into
// state. Adding or removing is a mutation that invalidates the shared key, so the chip list re-reads
// from the server. useState holds ONLY the tag the user has currently picked but not yet added — an
// uncommitted field. The picker lists every tag not already on the project, by id (what the test
// selects by).
export function ProjectTags({ projectId }: { projectId: number }) {
  const { data: assigned } = useProjectTags(projectId);
  const { data: allTags } = useAllTags();
  const addTag = useAddProjectTag(projectId);
  const removeTag = useRemoveProjectTag(projectId);
  const [picked, setPicked] = useState('');

  if (!assigned || !allTags) {
    return null;
  }

  const available = allTags.filter((tag) => !assigned.some((a) => a.id === tag.id));

  return (
    <section data-testid="project-tags">
      <ul data-testid="project-tags-list">
        {assigned.map((tag) => (
          <li key={tag.id}>
            <span data-testid={`project-tag-${tag.id}`}>{tag.name}</span>
            <button
              type="button"
              data-testid={`project-tag-remove-${tag.id}`}
              disabled={removeTag.isPending}
              onClick={() => removeTag.mutate({ tagId: tag.id })}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>
      <select
        data-testid="project-tag-add"
        value={picked}
        onChange={(event) => setPicked(event.target.value)}
      >
        <option value="">Add a tag…</option>
        {available.map((tag) => (
          <option key={tag.id} value={String(tag.id)}>
            {tag.name}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="project-tag-add-submit"
        disabled={!picked || addTag.isPending}
        onClick={() => {
          if (!picked) {
            return;
          }
          addTag.mutate(
            { tagId: Number(picked) },
            { onSuccess: () => setPicked('') },
          );
        }}
      >
        Add tag
      </button>
    </section>
  );
}
