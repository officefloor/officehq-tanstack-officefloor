import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { useSheetToday } from '../sheet/useSheetToday';
import { delegationsKey, delegationsQuery } from './delegationsQuery';
import type { Delegation } from './delegationsQuery';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

const isDate = (d: string) => DATE.test(d) && new Date(`${d}T00:00:00Z`).toISOString().slice(0, 10) === d;

/** The sheet's delegations of the steward role, and the form a steward delegates it through. While the sheet's today
 *  is inside a delegation's range (both dates inclusive) its user acts as a steward. A blank from-date is the sheet's
 *  today, a blank to-date the from-date; only a steward delegates, and a to-date before the from-date is refused. */
function Delegations({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const delegations = useQuery(delegationsQuery(sheetId));
  const today = useSheetToday(sheetId);
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: Omit<Delegation, 'by'>) =>
      postJson<Delegation>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: delegationsKey(sheetId) });
      setName('');
      setFrom('');
      setTo('');
    },
    onError: () => setError('The role could not be delegated: only a steward delegates, and the to-date may not be before the from-date.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const n = name.trim();
    const f = from.trim() || today;
    const t = to.trim() || f;
    if (n === '') {
      setError('A user name is needed.');
      return;
    }
    if (!isDate(f) || !isDate(t)) {
      setError('The dates must be yyyy-mm-dd.');
      return;
    }
    if (t < f) {
      setError('The to-date is before the from-date.');
      return;
    }
    once(() => save.mutateAsync({ user: n, from: f, to: t }));
  };

  const field = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <section data-testid="delegations">
      <h3>Delegations</h3>
      <form data-testid="delegate-form" onSubmit={submit}>
        <label>
          User <input data-testid="delegate-user" value={name} onChange={field(setName)} />
        </label>{' '}
        <label>
          From <input data-testid="delegate-from" placeholder={today} value={from} onChange={field(setFrom)} />
        </label>{' '}
        <label>
          To <input data-testid="delegate-to" placeholder="the from-date" value={to} onChange={field(setTo)} />
        </label>{' '}
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate steward role
        </button>
        {error && <p data-testid="delegate-error">{error}</p>}
      </form>
      {delegations.isError && <p data-testid="delegations-load-error">Could not load the delegations.</p>}
      <ul data-testid="delegations-list">
        {(delegations.data ?? []).map((d, i) => (
          <li key={i} data-testid="delegate-line">
            <span data-testid="delegate-line-user">{d.user}</span> acts as a steward from{' '}
            <span data-testid="delegate-line-from">{d.from}</span> to <span data-testid="delegate-line-to">{d.to}</span>,
            for <span data-testid="delegate-line-by">{d.by}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 6,
  Component: ({ sheetId }) => <Delegations key={`delegations-${sheetId}`} sheetId={sheetId} />,
});
