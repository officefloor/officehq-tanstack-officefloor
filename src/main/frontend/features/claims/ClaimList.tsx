import { asString, useSearchParam } from '../../url/useSearchParam';
import type { Claim } from './useClaims';

// A person's claims, newest first, each with its amount, date and note and an edit button that
// opens the claim in the person's claim form (cancel closes it again).
export function ClaimList({ claims }: { claims: Claim[] }) {
  const [editing, setEditing] = useSearchParam('editClaim', asString);
  return (
    <ul data-testid="claims">
      {claims.map((c) => (
        <li key={c.id} data-testid={`claim-row-${c.id}`}>
          <span data-testid="claim-amount">{c.amount}</span>{' '}
          <span data-testid="claim-date">{c.date}</span>{' '}
          <span data-testid="claim-note">{c.note}</span>{' '}
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
