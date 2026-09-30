import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { CLIENT_STATEMENT_PARAM } from './queries';
import { ClientStatementTable } from './ClientStatementTable';

// The statement panel on a client's detail page — one new *.slot.tsx file filling the ClientDetail
// region (CLAUDE.md rule 3). Nothing existing is edited to add it. The "open statement" control owns
// the `clientStatement` URL key (rule 4): whether the statement is open outlives a click, so it
// lives in the URL, and the panel reads the same key to decide whether to render the table.
function ClientStatementPanel({ clientId }: { clientId: number }) {
  const [open, setOpen] = useSearchParam(CLIENT_STATEMENT_PARAM, asFlag);
  return (
    <section data-testid="client-statement">
      <button
        type="button"
        data-testid="client-statement-open"
        onClick={() => setOpen(open ? undefined : true)}
      >
        Statement
      </button>
      {open ? <ClientStatementTable clientId={clientId} /> : null}
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 20, Component: ClientStatementPanel });
