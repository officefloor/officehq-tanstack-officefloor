import { ClientDetail } from '../../slots/defs/clientDetail';
import { asFlag, useSearchParam } from '../../url/useSearchParam';

// The show-all toggle for a client's projects — its own file, filling the client-detail slot above
// the projects panel (order 5, before the panel at 10). It OWNS the `clientProjectsShowAll` URL key
// (rule 4: a show/hide toggle outlives a click, so it lives in the URL, not useState). The projects
// panel reads the same key to decide whether the finished and archived projects are listed; nothing
// is passed between them. Carries data-testid="client-projects-show-all" (the test contract).
function ClientProjectsShowAll() {
  const [showAll, setShowAll] = useSearchParam('clientProjectsShowAll', asFlag);
  return (
    <button
      type="button"
      data-testid="client-projects-show-all"
      aria-pressed={showAll}
      onClick={() => setShowAll(showAll ? undefined : true)}
    >
      {showAll ? 'Show active only' : 'Show all projects'}
    </button>
  );
}

export const contribution = ClientDetail.fill({ order: 5, Component: ClientProjectsShowAll });
