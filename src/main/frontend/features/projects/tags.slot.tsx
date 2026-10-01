import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ProjectDetail } from '../../slots/defs/projectDetail';
import {
  tagsKey,
  projectTagsKey,
  listTags,
  listProjectTags,
  addProjectTag,
  removeProjectTag,
  type Tag,
} from './tagsApi';

// A project's tags, shown as chips with a remove control each, plus a select + button to add one —
// one panel filling the project.detail region. Reads two server lists: the project's own tags under
// ['projectTags', projectId] (the chips) and the full catalogue under ['tags'] (the add select, minus
// the ones already on the project). Both add and remove are mutations that invalidate
// ['projectTags', projectId] on success so this panel refetches; neither list is ever copied into
// state. The only useState is the select's uncommitted choice — what the user has picked but not yet
// submitted.
function ProjectTags({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const [choice, setChoice] = useState('');

  const { data: all } = useQuery({ queryKey: tagsKey, queryFn: listTags });
  const { data: tags } = useQuery({
    queryKey: projectTagsKey(projectId),
    queryFn: () => listProjectTags(projectId),
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: projectTagsKey(projectId) });

  const add = useMutation({
    mutationFn: (tagId: number) => addProjectTag(projectId, tagId),
    onSuccess: () => {
      setChoice('');
      void invalidate();
    },
  });
  const remove = useMutation({
    mutationFn: (tagId: number) => removeProjectTag(projectId, tagId),
    onSuccess: () => {
      void invalidate();
    },
  });

  if (!all || !tags) {
    return null;
  }

  const onProject = new Set(tags.map((tag: Tag) => tag.id));
  const available = all.filter((tag: Tag) => !onProject.has(tag.id));

  return (
    <section data-testid="project-tags">
      <h2>Tags</h2>
      <ul data-testid="project-tags-list">
        {tags.map((tag: Tag) => (
          <li key={tag.id}>
            <span data-testid={`project-tag-${tag.id}`}>{tag.name}</span>
            <button
              type="button"
              data-testid={`project-tag-remove-${tag.id}`}
              disabled={remove.isPending}
              onClick={() => remove.mutate(tag.id)}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>
      <select
        data-testid="project-tag-add"
        value={choice}
        onChange={(e) => setChoice(e.target.value)}
      >
        <option value="">Choose a tag…</option>
        {available.map((tag: Tag) => (
          <option key={tag.id} value={tag.id}>
            {tag.name}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="project-tag-add-submit"
        disabled={!choice || add.isPending}
        onClick={() => add.mutate(Number(choice))}
      >
        Add tag
      </button>
    </section>
  );
}

export const contribution = ProjectDetail.fill({ order: 40, Component: ProjectTags });
