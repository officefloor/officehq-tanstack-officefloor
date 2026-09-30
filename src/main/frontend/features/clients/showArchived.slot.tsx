import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { asFlag, useSearchParam } from '../../url/useSearchParam';

// The show-archived toggle — its own file, filling the clients toolbar slot. It OWNS the
// `showArchived` URL key (rule 4: a show/hide toggle outlives a click, so it lives in the URL, not
// useState). The archived-clients panel (features/clients/archived.slot.tsx) reads the same key to
// decide whether to reveal the tucked-away clients; nothing is passed between them. Carries
// data-testid="clients-show-archived" (the test contract).
function ShowArchived() {
  const [showArchived, setShowArchived] = useSearchParam('showArchived', asFlag);
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
