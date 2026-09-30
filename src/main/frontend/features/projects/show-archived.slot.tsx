import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { PROJECTS_SHOW_ARCHIVED_PARAM } from './queries';

// The "reveal archived projects" toggle — its own file filling the projects toolbar region
// (CLAUDE.md rule 3). It owns the `showArchived` URL key (rule 4): the choice outlives a click, so it
// lives in the URL, and the list (ProjectsTable) reads the very same key to decide whether archived
// rows show. No callback, no shared state — just the key.
function ShowArchivedToggle() {
  const [show, setShow] = useSearchParam(PROJECTS_SHOW_ARCHIVED_PARAM, asFlag);
  return (
    <label>
      <input
        type="checkbox"
        data-testid="projects-show-archived"
        checked={show}
        onChange={(event) => setShow(event.target.checked ? true : undefined)}
      />
      Show archived
    </label>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 10, Component: ShowArchivedToggle });
