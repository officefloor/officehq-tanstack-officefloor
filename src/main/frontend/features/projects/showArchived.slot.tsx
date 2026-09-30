import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { asFlag, useSearchParam } from '../../url/useSearchParam';

// The show-archived toggle — its own file, filling the projects toolbar slot. It OWNS the
// `showArchived` URL key (rule 4: a show/hide toggle outlives a click, so it lives in the URL, not
// useState). The projects page reads the same key to decide whether archived projects are listed;
// nothing is passed between them. Carries data-testid="projects-show-archived" (the test contract).
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
