import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { useUser } from '../session/useUser';
import { claimsKey, claimsQuery } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { periodOf, periodsQuery } from '../periods/periodsQuery';
import { ClaimForm } from './ClaimForm';
import { useClaimError } from './useClaimError';

/**
 * A claim's edit button, or its cancel button while the user's editor is open on it. Opening the editor locks the claim
 * on the server for everyone else until it is saved or cancelled (or the sheet's today changes); the server refuses a
 * viewer, a claim that cannot be edited and one another user has locked, and the refusal shows on the claim (in its shared `claim-error`).
 */
function ClaimEditCell({ sheetId, claim, editing }: { sheetId: string; claim: Claim; editing: boolean }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const once = useSubmitOnce();
  const path = `/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}`;
  const lock = useMutation({
    mutationFn: (to: 'lock' | 'unlock') => putJson<Claim>(`${path}/${to}`, {}, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e, to) =>
      setError(
        e instanceof RefusedError
          ? e.message
          : to === 'lock'
            ? 'The claim could not be opened for editing.'
            : 'The claim editor could not be closed.',
      ),
  });
  const periods = useQuery(periodsQuery(sheetId)).data ?? [];
  // What the screen already knows refuses at once, before anything is sent; the server checks the rest (and these again).
  const refusal = () => {
    if (claim.status !== 'DRAFT') return `A ${claim.status} claim cannot be edited: only a draft can.`;
    const period = periodOf(periods, claim.date);
    if (period?.closed) return `The period ${period.name} is closed: its claims cannot change.`;
    if (claim.lockedBy !== null && claim.lockedBy !== user) return `The claim is being edited by ${claim.lockedBy}.`;
    return '';
  };
  const press = (to: 'lock' | 'unlock') => {
    const refused = to === 'lock' ? refusal() : '';
    setError(refused);
    if (!refused) once(() => lock.mutateAsync(to));
  };

  return (
    <td>
      {editing ? (
        <button type="button" data-testid="claim-cancel" disabled={lock.isPending} onClick={() => press('unlock')}>
          Cancel
        </button>
      ) : (
        <button type="button" data-testid="claim-edit" disabled={lock.isPending} onClick={() => press('lock')}>
          Edit
        </button>
      )}
    </td>
  );
}

/**
 * A person's claims, newest first, with the form they are added and edited through. The editor is open on the claim
 * the signed-in user holds the lock of, so it is still open after the sheet is re-loaded.
 */
function PersonClaims({ sheetId, personId }: { sheetId: string; personId: number }) {
  const [user] = useUser();
  const claims = (useQuery(claimsQuery(sheetId)).data ?? []).filter((c) => c.personId === personId);
  const editing = claims.find((c) => c.lockedBy !== null && c.lockedBy === user);

  return (
    <td data-testid="person-claims">
      <table>
        <tbody>
          {claims.map((c) => (
            <tr key={c.id} data-testid={`claim-row-${c.id}`}>
              <td data-testid="claim-number">{c.number ?? ''}</td>
              <td data-testid="claim-amount">{c.amount}</td>
              <td data-testid="claim-date">{c.date}</td>
              <td data-testid="claim-note">{c.note}</td>
              <td data-testid="claim-corrects">{c.corrects ?? ''}</td>
              <ClaimCells.Slot sheetId={sheetId} claim={c} />
              <ClaimEditCell sheetId={sheetId} claim={c} editing={c.id === editing?.id} />
            </tr>
          ))}
        </tbody>
      </table>
      <ClaimForm sheetId={sheetId} personId={personId} editing={editing} onDone={() => undefined} />
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 20, Component: () => <th>Claims</th> }),
  PersonCells.fill({ order: 20, Component: PersonClaims }),
];
