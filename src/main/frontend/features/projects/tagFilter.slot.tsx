import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { asString, useSearchParam } from '../../url/useSearchParam';
import type { Tag } from './ProjectTagsPanel';

// The by-tag project filter — its own file, filling the projects toolbar slot (order 20, after the
// show-archived toggle at 10). It OWNS the `projectTag` URL key (rule 4: a filter outlives a click,
// so it lives in the URL, not useState). The projects page reads the same key and narrows its rows;
// nothing is passed between them. The options come from useQuery under ['tags'] — the SAME key the
// tags feature owns — so a newly created tag appears here too. Carries data-testid="project-tag-filter"
// (the test contract); the empty option clears the key, showing every project again.
function TagFilter() {
  const [tag, setTag] = useSearchParam('projectTag', asString);
  const tags = useQuery({
    queryKey: ['tags'],
    queryFn: () => getJson<Tag[]>('/api/tags'),
  });
  return (
    <select
      data-testid="project-tag-filter"
      value={tag}
      onChange={(e) => setTag(e.target.value || undefined)}
    >
      <option value="">All tags</option>
      {(tags.data ?? []).map((t) => (
        <option key={t.id} value={String(t.id)}>
          {t.name}
        </option>
      ))}
    </select>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 20, Component: TagFilter });
