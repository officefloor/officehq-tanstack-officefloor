import { useQuery } from '@tanstack/react-query';
import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asNumber } from '../../url/useSearchParam';
import { PROJECTS_TAG_FILTER_PARAM, tagsKey, fetchTagOptions, type TagOption } from './queries';

// The "filter projects by label" select — its own file filling the projects toolbar region
// (CLAUDE.md rule 3). It owns the `tag` URL key (rule 4): the choice outlives a click, so it lives
// in the URL, and the list (ProjectsTable) reads the very same key to decide which rows show. No
// callback, no shared state — just the key. Its options come from the shared ['tags'] pool (rule 5).
function TagFilter() {
  const { data: tags } = useQuery({ queryKey: tagsKey, queryFn: fetchTagOptions });
  const [selected, setSelected] = useSearchParam(PROJECTS_TAG_FILTER_PARAM, asNumber);

  return (
    <label>
      Label
      <select
        data-testid="project-tag-filter"
        value={selected ?? ''}
        onChange={(event) => setSelected(event.target.value ? Number(event.target.value) : undefined)}
      >
        <option value="">All labels</option>
        {(tags ?? []).map((tag: TagOption) => (
          <option key={tag.id} value={tag.id}>
            {tag.name}
          </option>
        ))}
      </select>
    </label>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 20, Component: TagFilter });
