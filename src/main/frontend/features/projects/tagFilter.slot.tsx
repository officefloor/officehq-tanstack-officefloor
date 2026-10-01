import { useQuery } from '@tanstack/react-query';
import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asNumber } from '../../url/useSearchParam';
import { tagsKey, listTags, type Tag } from './tagsApi';

// A control over the projects list — one new *.slot.tsx filling the projects.toolbar region. It owns
// the `projectTag` URL key; the list reads the same key and keeps only the projects carrying that
// tag. Nothing is passed between them: they stay in step through the shared search param. The empty
// option clears the key, so the list falls back to showing every project. The options come from the
// tag catalogue under ['tags'] (shared by KEY with the add-tag panel, not an import), so a new tag
// appears here too.
function ProjectTagFilter() {
  const [tagId, setTagId] = useSearchParam('projectTag', asNumber);
  const { data: tags } = useQuery({ queryKey: tagsKey, queryFn: listTags });

  return (
    <select
      data-testid="project-tag-filter"
      value={tagId ?? ''}
      onChange={(e) => setTagId(asNumber(e.target.value))}
    >
      <option value="">All tags</option>
      {(tags ?? []).map((tag: Tag) => (
        <option key={tag.id} value={tag.id}>
          {tag.name}
        </option>
      ))}
    </select>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 20, Component: ProjectTagFilter });
