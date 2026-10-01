import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// The control that opens a client's statement — one new *.slot.tsx filling the client.detail region.
// Showing the statement is a show/hide toggle, so it lives in the URL: this button owns the
// `statement` key, and the statement panel reads the same key. Nothing is passed between them; they
// stay in step through the shared search param. Off clears the key, tucking the statement away.
function OpenStatement() {
  const [open, setOpen] = useSearchParam('statement', asFlag);
  return (
    <button
      data-testid="client-statement-open"
      type="button"
      aria-pressed={open}
      onClick={() => setOpen(open ? undefined : true)}
    >
      {open ? 'Hide statement' : 'Statement'}
    </button>
  );
}

export const contribution = ClientDetail.fill({ order: 25, Component: OpenStatement });
