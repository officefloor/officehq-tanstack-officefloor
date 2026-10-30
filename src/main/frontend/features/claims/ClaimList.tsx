import { asString, useSearchParam } from '../../url/useSearchParam';
import { dueCents, showDue, toCents } from '../rates/due';
import type { Claim } from './useClaims';

// A person's claims, newest first, each with its amount, date, note and due (its amount at the
// person's grade's rate, n/a when the grade has none) and an edit button that
// opens the claim in the person's claim form (cancel closes it again).
export function ClaimList({ claims, rate }: { claims: Claim[]; rate: string | undefined }) {
  const [editing, setEditing] = useSearchParam('editClaim', asString);
  return (
    <ul data-testid="claims">
      {claims.map((c) => (
        <li key={c.id} data-testid={`claim-row-${c.id}`}>
          <span data-testid="claim-amount">{c.amount}</span>{' '}
          <span data-testid="claim-date">{c.date}</span>{' '}
          <span data-testid="claim-note">{c.note}</span>{' '}
          <span data-testid="claim-due">{showDue(dueCents(toCents(c.amount), rate))}</span>{' '}
          {editing === c.id ? (
            <button type="button" data-testid="claim-cancel" onClick={() => setEditing(undefined)}>
              Cancel
            </button>
          ) : (
            <button type="button" data-testid="claim-edit" onClick={() => setEditing(c.id)}>
              Edit
            </button>
          )}
        </li>
      ))}
    </ul>
  );
}
