import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// "Show archived" — a self-contained control that owns the `clientsShowArchived` URL key. Archived
// clients are hidden by default; the clients list reads the same key and, when it is on, reveals
// archived rows too (each carrying its own restore action). The two share only the key, no import.
// The flag lives in the URL so the revealed view outlives the click and is shareable.
function ShowArchived() {
  const [showArchived, setShowArchived] = useSearchParam('clientsShowArchived', asFlag);
  return (
    <button
      type="button"
      data-testid="clients-show-archived"
      aria-pressed={showArchived}
      onClick={() => setShowArchived(showArchived ? undefined : true)}
    >
      {showArchived ? 'Hide archived' : 'Show archived'}
    </button>
  );
}

export const contribution = ClientsToolbar.fill({ order: 10, Component: ShowArchived });
