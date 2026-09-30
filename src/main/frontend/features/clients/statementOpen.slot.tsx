import { ClientDetail } from '../../slots/defs/clientDetail';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The control that opens a client's statement — its own file, filling the client-detail slot. It
// OWNS the `statement` URL key (rule 4: a show/hide toggle outlives a click, so it lives in the URL,
// not useState). The statement panel (features/clients/statement.slot.tsx) reads the same key to
// decide whether to show the statement; nothing is passed between them. Carries
// data-testid="client-statement-open" (the test contract).
function StatementOpen() {
  const [statement, setStatement] = useSearchParam('statement', asString);
  const open = statement === 'open';
  return (
    <button
      type="button"
      data-testid="client-statement-open"
      aria-pressed={open}
      onClick={() => setStatement(open ? undefined : 'open')}
    >
      {open ? 'Hide statement' : 'Statement'}
    </button>
  );
}

export const contribution = ClientDetail.fill({ order: 20, Component: StatementOpen });
