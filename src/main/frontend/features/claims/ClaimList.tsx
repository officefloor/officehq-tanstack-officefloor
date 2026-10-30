import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimDue, showDue } from '../rates/due';
import { claimsKey, type Claim } from './useClaims';

// A person's claims, newest first, each with its amount, date, note, status, number, opened-by, stated (yes when the
// person's last statement bound it) and due (fixed when it was settled, else bound by the last statement; otherwise its amount at the person's grade's rate in force on the claim's date, n/a when none applies), the status buttons (open, settle, void —
// one the claim's status does not allow is refused with the claim's error) and an edit button that
// opens the claim in the person's claim form (cancel closes it again).
export function ClaimList({ claims, rateOn }: { claims: Claim[]; rateOn: (date: string) => string | undefined }) {
  return (
    <ul data-testid="claims">
      {claims.map((c) => (
        <ClaimRow key={c.id} claim={c} rate={rateOn(c.date)} />
      ))}
    </ul>
  );
}

function ClaimRow({ claim: c, rate }: { claim: Claim; rate: string | undefined }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [editing, setEditing] = useSearchParam('editClaim', asString);
  const queryClient = useQueryClient();

  const status = useMutation({
    mutationFn: (action: 'open' | 'settle' | 'void') =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/status`,
        { action, user },
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // The next action on the row removes an old error.
  const act = (run: () => void) => {
    status.reset();
    run();
  };

  return (
    <li data-testid={`claim-row-${c.id}`}>
      <span data-testid="claim-amount">{c.amount}</span>{' '}
      <span data-testid="claim-date">{c.date}</span>{' '}
      <span data-testid="claim-note">{c.note}</span>{' '}
      <span data-testid="claim-status">{c.status}</span>{' '}
      <span data-testid="claim-number">{c.number}</span>{' '}
      <span data-testid="claim-due">{showDue(claimDue(c, rate))}</span>{' '}
      <span data-testid="claim-opened-by">{c.openedBy}</span>{' '}
      <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="claim-open" onClick={() => act(() => status.mutate('open'))}>
        Open
      </button>{' '}
      <button type="button" data-testid="claim-settle" onClick={() => act(() => status.mutate('settle'))}>
        Settle
      </button>{' '}
      <button type="button" data-testid="claim-void" onClick={() => act(() => status.mutate('void'))}>
        Void
      </button>{' '}
      {editing === c.id ? (
        <button type="button" data-testid="claim-cancel" onClick={() => act(() => setEditing(undefined))}>
          Cancel
        </button>
      ) : (
        <button type="button" data-testid="claim-edit" onClick={() => act(() => setEditing(c.id))}>
          Edit
        </button>
      )}
      {status.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: a {c.status} claim cannot make that status change (the user who opened a claim cannot settle it).
        </span>
      )}
    </li>
  );
}
