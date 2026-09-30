import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  projectTagsKey,
  allTagsKey,
  fetchProjectTags,
  fetchAllTags,
  addProjectTag,
  removeProjectTag,
  type Tag,
} from './queries';

// A project's labels: the chips it carries, plus a picker to add one and a per-chip remove button.
// Queries for itself under ['projects', projectId, 'tags'] and the shared pool under ['tags'] —
// never handed its data by a parent (CLAUDE.md rule 5). Adding/removing a label is a mutation that
// invalidates the per-project key so the chips refresh themselves (rule 5). The picker's current
// choice is uncommitted field input, so it (and only it) lives in useState (rule 4). Carries the
// stable data-testid contract the test reads: a chip per tag, its remove button, the picker and its
// submit button.
export function ProjectTags({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const { data: tags } = useQuery({
    queryKey: projectTagsKey(projectId),
    queryFn: () => fetchProjectTags(projectId),
  });
  const { data: allTags } = useQuery({ queryKey: allTagsKey, queryFn: fetchAllTags });

  const [selected, setSelected] = useState('');

  const invalidate = () =>
    void queryClient.invalidateQueries({ queryKey: projectTagsKey(projectId) });
  const addMutation = useMutation({
    mutationFn: (tagId: number) => addProjectTag(projectId, tagId),
    onSuccess: invalidate,
  });
  const removeMutation = useMutation({
    mutationFn: (tagId: number) => removeProjectTag(projectId, tagId),
    onSuccess: invalidate,
  });

  if (!tags || !allTags) {
    return null;
  }

  const assigned = new Set(tags.map((tag: Tag) => tag.id));
  const available = allTags.filter((tag: Tag) => !assigned.has(tag.id));

  return (
    <section data-testid="project-tags">
      <ul>
        {tags.map((tag: Tag) => (
          <li key={tag.id}>
            <span data-testid={`project-tag-${tag.id}`}>{tag.name}</span>
            <button
              data-testid={`project-tag-remove-${tag.id}`}
              type="button"
              onClick={() => removeMutation.mutate(tag.id)}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>
      <select
        data-testid="project-tag-add"
        value={selected}
        onChange={(e) => setSelected(e.target.value)}
      >
        <option value="">Add a label…</option>
        {available.map((tag: Tag) => (
          <option key={tag.id} value={tag.id}>
            {tag.name}
          </option>
        ))}
      </select>
      <button
        data-testid="project-tag-add-submit"
        type="button"
        onClick={() => {
          if (selected) {
            addMutation.mutate(Number(selected));
            setSelected('');
          }
        }}
      >
        Add
      </button>
    </section>
  );
}
