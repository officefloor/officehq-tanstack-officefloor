import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// "Show archived" — a self-contained control that owns the `showArchived` URL key. Archived projects
// are hidden by default; the projects list reads the same key and, when it is on, reveals archived
// rows too. The two share only the key, no import. The flag lives in the URL so the revealed view
// outlives the click and is shareable.
function ShowArchived() {
  const [showArchived, setShowArchived] = useSearchParam('showArchived', asFlag);
  return (
    <button
      type="button"
      data-testid="projects-show-archived"
      aria-pressed={showArchived}
      onClick={() => setShowArchived(showArchived ? undefined : true)}
    >
      {showArchived ? 'Hide archived' : 'Show archived'}
    </button>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 10, Component: ShowArchived });
