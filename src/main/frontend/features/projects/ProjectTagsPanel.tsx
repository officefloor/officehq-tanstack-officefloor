import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A tag as the server returns it: an id and a name. Tags are reusable labels shared across projects.
export type Tag = {
  id: number;
  name: string;
};

// A project↔tag pairing, with the tag's name resolved so a chip can render its label directly.
export type ProjectTag = {
  projectId: number;
  tagId: number;
  name: string;
};

// A project's tags, rendered in the project-detail context: the chips already on this project, a
// per-chip remove button, and a select of the remaining tags to add one. Both lists come from
// useQuery — every tag under ['tags'], every pairing under ['projectTags'] — and are filtered to
// this project here (rule 5); nothing is copied into state or hand-maintained. Adding and removing
// are useMutations that invalidate ['projectTags'], so the chips refetch themselves. The only
// useState is the tag the user has currently picked but not yet added (rule 4).
export function ProjectTagsPanel({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const tags = useQuery({
    queryKey: ['tags'],
    queryFn: () => getJson<Tag[]>('/api/tags'),
  });
  const projectTags = useQuery({
    queryKey: ['projectTags'],
    queryFn: () => getJson<ProjectTag[]>('/api/project-tags'),
  });

  const [selected, setSelected] = useState('');

  const add = useMutation({
    mutationFn: (tagId: number) =>
      postJson<ProjectTag>('/api/project-tags', { projectId, tagId }),
    onSuccess: () => {
      setSelected('');
      void queryClient.invalidateQueries({ queryKey: ['projectTags'] });
    },
  });

  const remove = useMutation({
    mutationFn: (tagId: number) =>
      postJson<ProjectTag>('/api/project-tags/remove', { projectId, tagId }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['projectTags'] });
    },
  });

  const assigned = (projectTags.data ?? []).filter((pt) => pt.projectId === projectId);
  const assignedIds = new Set(assigned.map((pt) => pt.tagId));
  const available = (tags.data ?? []).filter((t) => !assignedIds.has(t.id));

  return (
    <section data-testid="project-tags">
      <ul data-testid="project-tags-list">
        {assigned.map((pt) => (
          <li key={pt.tagId} data-testid={`project-tag-row-${pt.tagId}`}>
            <span data-testid={`project-tag-${pt.tagId}`}>{pt.name}</span>
            <button
              type="button"
              data-testid={`project-tag-remove-${pt.tagId}`}
              onClick={() => remove.mutate(pt.tagId)}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>

      <form
        data-testid="project-tag-add-form"
        onSubmit={(e) => {
          e.preventDefault();
          const tagId = Number(selected);
          if (!tagId) {
            return;
          }
          add.mutate(tagId);
        }}
      >
        <select
          data-testid="project-tag-add"
          value={selected}
          onChange={(e) => setSelected(e.target.value)}
        >
          <option value="">Add a tag…</option>
          {available.map((t) => (
            <option key={t.id} value={String(t.id)}>
              {t.name}
            </option>
          ))}
        </select>
        <button data-testid="project-tag-add-submit" type="submit">
          Add tag
        </button>
      </form>
    </section>
  );
}
