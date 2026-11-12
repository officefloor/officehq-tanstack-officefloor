import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import type { Person } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { claimDue, useRates } from '../rates/rates';
import { useSettings } from '../settings/settings';
import { periodOf, usePeriods } from '../periods/periods';
import { useSheetToday } from '../sheet/sheet';
import { dueBy, lateOf, overdue } from '../overdue/overdue';
import { type Claim, type ClaimStatus, claimsKey, useClaims, useSelectedClaims } from './claims';

type Fields = { amount: string; date: string; note: string; corrects: string };
type StatusChange = { id: number; status: ClaimStatus };
const EMPTY: Fields = { amount: '', date: '', note: '', corrects: '' };

function ClaimsHead() {
  return <th>Claims</th>;
}

// A person's claims, newest first, each with its status and the buttons that move it on, and the
// form that adds one or edits the one whose editor is open.
function PersonClaims({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const settings = useSettings(sheet).data;
  const floor = settings?.floor;
  const terms = settings?.terms;
  const today = useSheetToday(sheet);
  const periods = usePeriods(sheet);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [editing, setEditing] = useState<number | undefined>();
  const [selected, setSelected] = useSelectedClaims();
  const queryClient = useQueryClient();
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;

  const save = useMutation({
    mutationFn: (f: Fields) =>
      editing !== undefined
        ? putJson<Claim>(`${base}/claims/${editing}`, { ...f, user })
        : postJson<Claim>(`${base}/people/${person.id}/claims`, { ...f, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      setFields(EMPTY);
      setEditing(undefined);
    },
  });

  const submit = useSubmitOnce(save);

  // The server refuses a change the claim's status does not allow; the error shows on that claim.
  const move = useMutation({
    mutationFn: (m: StatusChange) => postJson<Claim>(`${base}/claims/${m.id}/status`, { status: m.status, user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });
  const moveTo = (id: number, status: ClaimStatus) => {
    move.reset();
    move.mutate({ id, status });
  };

  const set = (key: keyof Fields) => (e: ChangeEvent<HTMLInputElement>) => {
    setFields({ ...fields, [key]: e.target.value });
    save.reset();
  };
  const open = (c: Claim) => {
    save.reset();
    move.reset();
    setEditing(c.id);
    setFields({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects ?? '' });
  };
  const cancel = () => {
    save.reset();
    setEditing(undefined);
    setFields(EMPTY);
  };

  return (
    <td>
      <ul>
        {(claims.data ?? [])
          .filter((c) => c.personId === person.id)
          .map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <input
                type="checkbox"
                data-testid="claim-select"
                aria-label="Select claim"
                checked={selected.includes(c.id)}
                onChange={(e) => setSelected(e.target.checked ? [...selected, c.id] : selected.filter((id) => id !== c.id))}
              />{' '}
              <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
              <span data-testid="claim-period">{periodOf(periods.data, c.date)?.name ?? ''}</span>{' '}
              <span data-testid="claim-due">{claimDue(c, rates.data, person.grades, floor, lateOf(settings, today))}</span>{' '}
              <span data-testid="claim-due-by">{dueBy(c, terms)}</span>{' '}
              <span data-testid="claim-overdue">{overdue(c, terms, today) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              <button type="button" data-testid="claim-open" disabled={move.isPending} onClick={() => moveTo(c.id, 'OPEN')}>
                Open
              </button>{' '}
              <button type="button" data-testid="claim-settle" disabled={move.isPending} onClick={() => moveTo(c.id, 'SETTLED')}>
                Settle
              </button>{' '}
              <button type="button" data-testid="claim-void" disabled={move.isPending} onClick={() => moveTo(c.id, 'VOID')}>
                Void
              </button>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                Edit
              </button>
              {editing === c.id && (
                <button type="button" data-testid="claim-cancel" onClick={cancel}>
                  Cancel
                </button>
              )}
              {move.isError && move.variables?.id === c.id && (
                <span data-testid="claim-error" role="alert">
                  {' '}
                  {move.error instanceof HttpError && move.error.reason ? `${move.error.reason}.` : 'The change was refused.'}
                </span>
              )}
            </li>
          ))}
      </ul>
      <form
        data-testid="claim-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(fields);
        }}
      >
        <input data-testid="claim-form-amount" placeholder="Amount" inputMode="decimal" value={fields.amount} onChange={set('amount')} />
        <input data-testid="claim-form-date" placeholder="Date (yyyy-mm-dd)" value={fields.date} onChange={set('date')} />
        <input data-testid="claim-form-note" placeholder="Note" value={fields.note} onChange={set('note')} />
        <input
          data-testid="claim-form-corrects"
          placeholder="Corrects claim no."
          inputMode="numeric"
          value={fields.corrects}
          onChange={set('corrects')}
        />
        <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
          {editing !== undefined ? 'Save claim' : 'Add claim'}
        </button>
        {save.isError && (
          <span data-testid="claim-form-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The claim was refused.'}
          </span>
        )}
      </form>
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 20, Component: ClaimsHead }),
  PersonColumns.fill({ order: 20, Component: PersonClaims }),
];
