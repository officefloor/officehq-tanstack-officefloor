import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// A control over the clients list — one new *.slot.tsx filling the clients.toolbar region. It owns
// the `showArchived` URL key; the list reads the same key and reveals the tucked-away clients when
// it is on. Nothing is passed between them: they stay in step through the shared search param. Off
// clears the key, so the list falls back to showing only the active clients.
function ShowArchivedToggle() {
  const [showArchived, setShowArchived] = useSearchParam('showArchived', asFlag);
  return (
    <button
      data-testid="clients-show-archived"
      type="button"
      aria-pressed={showArchived}
      onClick={() => setShowArchived(showArchived ? undefined : true)}
    >
      {showArchived ? 'Hide archived' : 'Show archived'}
    </button>
  );
}

export const contribution = ClientsToolbar.fill({ order: 10, Component: ShowArchivedToggle });
