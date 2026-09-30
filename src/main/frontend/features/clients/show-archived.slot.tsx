import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { CLIENTS_SHOW_ARCHIVED_PARAM } from './queries';

// The "reveal tucked-away clients" toggle — its own file filling the clients toolbar region
// (CLAUDE.md rule 3), so the page and the list are never edited to add it. It owns the
// `clientsArchived` URL key (rule 4): the choice outlives a click, so it lives in the URL, and the
// list (ClientsTable) reads the very same key to decide whether archived rows show. No callback, no
// shared state — just the key. Carries data-testid="clients-show-archived" (the test contract).
function ShowArchivedToggle() {
  const [show, setShow] = useSearchParam(CLIENTS_SHOW_ARCHIVED_PARAM, asFlag);
  return (
    <label>
      <input
        type="checkbox"
        data-testid="clients-show-archived"
        checked={show}
        onChange={(event) => setShow(event.target.checked ? true : undefined)}
      />
      Show archived
    </label>
  );
}

export const contribution = ClientsToolbar.fill({ order: 10, Component: ShowArchivedToggle });
