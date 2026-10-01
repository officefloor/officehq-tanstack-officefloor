import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// A control over the projects list — one new *.slot.tsx filling the projects.toolbar region. It owns
// the `showArchived` URL key; the list reads the same key and reveals the tucked-away projects when
// it is on. Nothing is passed between them: they stay in step through the shared search param. Off
// clears the key, so the list falls back to showing only the active projects.
function ShowArchivedToggle() {
  const [showArchived, setShowArchived] = useSearchParam('showArchived', asFlag);
  return (
    <button
      data-testid="projects-show-archived"
      type="button"
      aria-pressed={showArchived}
      onClick={() => setShowArchived(showArchived ? undefined : true)}
    >
      {showArchived ? 'Hide archived' : 'Show archived'}
    </button>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 10, Component: ShowArchivedToggle });
