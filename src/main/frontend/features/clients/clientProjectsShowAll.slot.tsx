import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// A control over the client's projects panel — one new *.slot.tsx filling the client.detail region
// (ordered just above the projects table). It owns the `clientProjectsAll` URL key; the projects
// panel reads the same key and reveals the finished and hidden (archived) projects when it is on.
// Nothing is passed between them: they stay in step through the shared search param. Off clears the
// key, so the panel falls back to showing only the active projects.
function ClientProjectsShowAll() {
  const [showAll, setShowAll] = useSearchParam('clientProjectsAll', asFlag);
  return (
    <button
      data-testid="client-projects-show-all"
      type="button"
      aria-pressed={showAll}
      onClick={() => setShowAll(showAll ? undefined : true)}
    >
      {showAll ? 'Show active only' : 'Show all jobs'}
    </button>
  );
}

export const contribution = ClientDetail.fill({ order: 15, Component: ClientProjectsShowAll });
