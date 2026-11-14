import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { listPeriods, periodOf, periodsKey } from '../periods/api';
import { claimDue, showDue, type GradeOn, type Late, type Rate } from '../rates/api';
import { addClaim, claimsKey, dueBy, isOverdue, moveClaim, updateClaim, type Claim, type ClaimAction, type ClaimInput } from './api';

const EMPTY: ClaimInput = { amount: '', date: '', note: '', corrects: '' };

/** A person's claims: a form that adds a claim or edits one, and the claims newest first, each
 *  with its status, its due (fixed when settled, else at the rate in force on the claim's date of the
 *  person's grade in force then (`gradeOn`) and at least the sheet's `floor`, or bound by the person's last statement; none when
 *  there is no such rate; an overdue one gains its late charge on top, see `charges`), its due-by (its date plus the sheet's `terms` in days) and whether it is
 *  overdue (OPEN and due-by before the sheet's `today`), whether
 *  it is stated,
 *  the period its date falls in (none when no period contains it), who opened it, and buttons that
 *  change its status (the user who opened a claim cannot settle it, nor a clerk one whose due is
 *  above the sheet's clerk limit), a checkbox that selects it for a bulk action, and for a correction (a negative amount, no floor and no late
 *  charge) the number of the SETTLED claim it corrects. Only a DRAFT can be edited:
 *  editing any other claim is refused with an error on the claim. */
export function PersonClaims({ sheetId, user, personId, claims, gradeOn, rates, floor, terms, today, charges, selected, onSelect }: {
  sheetId: string; user: string; personId: number; claims: Claim[]; gradeOn: GradeOn; rates: Rate[] | undefined;
  floor: string; terms: number; today: string; charges: Late;
  /** The claims selected for a bulk action, and how one is selected or unselected. */
  selected: ReadonlySet<number>; onSelect: (id: number, on: boolean) => void;
}) {
  const queryClient = useQueryClient();
  const periods = useQuery({ queryKey: periodsKey(sheetId), queryFn: () => listPeriods(sheetId) });
  const [form, setForm] = useState<ClaimInput>(EMPTY);
  const [editing, setEditing] = useState<number | null>(null);
  // The claim whose edit was refused (it is not a DRAFT); cleared by the next action on any claim here.
  const [editRefused, setEditRefused] = useState<number | null>(null);

  const save = useMutation({
    mutationFn: (v: { id: number | null; input: ClaimInput }) =>
      v.id === null ? addClaim(sheetId, user, personId, v.input) : updateClaim(sheetId, user, v.id, v.input),
    onSuccess: () => {
      setForm(EMPTY);
      setEditing(null);
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();
  const actOnce = useSubmitOnce();

  // The claim whose last status change was refused; cleared by the next action on any claim here.
  const move = useMutation({
    mutationFn: (v: { id: number; action: ClaimAction }) => moveClaim(sheetId, user, v.id, v.action),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const act = (id: number, action: ClaimAction) => {
    actOnce(() => {
      move.reset();
      setEditRefused(null);
      return move.mutateAsync({ id, action });
    });
  };

  const change = (patch: Partial<ClaimInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  const open = (c: Claim) => {
    move.reset();
    if (c.status !== 'DRAFT') {
      setEditRefused(c.id);
      return;
    }
    setEditRefused(null);
    setForm({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects === null ? '' : String(c.corrects) });
    setEditing(c.id);
    save.reset();
  };

  const cancel = () => {
    setForm(EMPTY);
    setEditing(null);
    save.reset();
  };

  return (
    <div>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const v = { id: editing, input: form };
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(v);
          });
        }}
      >
        <input data-testid="claim-form-amount" placeholder="Amount" value={form.amount}
          onChange={(e) => change({ amount: e.target.value })} />{' '}
        <input type="date" data-testid="claim-form-date" value={form.date}
          onChange={(e) => change({ date: e.target.value })} />{' '}
        <input data-testid="claim-form-note" placeholder="Note" value={form.note}
          onChange={(e) => change({ note: e.target.value })} />{' '}
        <input data-testid="claim-form-corrects" placeholder="Corrects no." value={form.corrects}
          onChange={(e) => change({ corrects: e.target.value })} />{' '}
        <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add claim' : 'Save claim'}
        </button>
        {save.isError && (
          <span data-testid="claim-form-error">{save.error instanceof HttpError && save.error.status === 423
            ? 'A claim dated inside a closed period cannot be added or edited.'
            : save.error instanceof HttpError && save.error.status === 409
            ? 'Only a DRAFT claim can be edited.'
            : 'A claim needs an amount with up to two decimals; a negative one (a correction) must name, by number, a SETTLED claim of this person.'}</span>
        )}
      </form>
      <table>
        <tbody>
          {claims.map((c) => (
            <tr key={c.id} data-testid={`claim-row-${c.id}`}>
              <td>
                <input type="checkbox" data-testid="claim-select" checked={selected.has(c.id)}
                  onChange={(e) => onSelect(c.id, e.target.checked)} />
              </td>
              <td data-testid="claim-number">{c.number ?? ''}</td>
              <td data-testid="claim-amount">{Number(c.amount).toFixed(2)}</td>
              <td data-testid="claim-date">{c.date}</td>
              <td data-testid="claim-note">{c.note}</td>
              <td data-testid="claim-corrects">{c.corrects ?? ''}</td>
              <td data-testid="claim-due">{showDue(claimDue(c, rates, gradeOn, floor, charges))}</td>
              <td>
                Due by <span data-testid="claim-due-by">{dueBy(c, terms)}</span>{' '}
                overdue <span data-testid="claim-overdue">{isOverdue(c, terms, today) ? 'yes' : 'no'}</span>
              </td>
              <td data-testid="claim-status">{c.status}</td>
              <td data-testid="claim-opened-by">{c.openedBy ?? ''}</td>
              <td>
                Stated <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>
              </td>
              <td data-testid="claim-period">{periodOf(periods.data, c.date)?.name ?? ''}</td>
              <td>
                {(['open', 'settle', 'void'] as const).map((a) => (
                  <button key={a} type="button" data-testid={`claim-${a}`} disabled={move.isPending}
                    onClick={() => act(c.id, a)}>
                    {a === 'open' ? 'Open' : a === 'settle' ? 'Settle' : 'Void'}
                  </button>
                ))}{' '}
                <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                  Edit
                </button>
                {editing === c.id && (
                  <button type="button" data-testid="claim-cancel" onClick={cancel}>
                    Cancel
                  </button>
                )}
                {editRefused === c.id && (
                  <span data-testid="claim-error">
                    {`A ${c.status} claim cannot be edited. To change it, void it and add a new one.`}
                  </span>
                )}
                {move.isError && move.variables?.id === c.id && (
                  <span data-testid="claim-error">{move.error instanceof HttpError && move.error.status === 423
                    ? 'A claim dated inside a closed period cannot be changed.'
                    : move.error instanceof HttpError && move.error.status === 403
                    ? 'A clerk cannot settle a claim whose due is above the clerk limit.'
                    : c.status === 'OPEN' && move.variables.action === 'settle'
                    ? 'The user who opened a claim cannot settle it.'
                    : `A ${c.status} claim cannot be ${move.variables.action === 'open'
                      ? 'opened' : move.variables.action === 'settle' ? 'settled' : 'voided'}.`}</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
