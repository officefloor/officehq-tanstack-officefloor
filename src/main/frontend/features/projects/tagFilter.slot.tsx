import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { useAllTags } from './tags';

// "Show just the projects carrying one label" — a self-contained control that owns the `projectTag`
// URL key. Its options are the full tag catalogue (the shared ['tags'] key), so new tags appear
// without touching this file. The projects list reads the same key and keeps only projects whose
// tagIds include it; the two share only the key, no import. Choosing a tag commits it to the URL (so
// the narrowed view outlives the click and is shareable); the empty option clears the key.
function TagFilter() {
  const [tag, setTag] = useSearchParam('projectTag', asString);
  const { data: tags } = useAllTags();
  return (
    <select
      data-testid="project-tag-filter"
      value={tag}
      onChange={(e) => setTag(e.target.value)}
    >
      <option value="">All tags</option>
      {(tags ?? []).map((t) => (
        <option key={t.id} value={String(t.id)}>
          {t.name}
        </option>
      ))}
    </select>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 20, Component: TagFilter });
