import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';

/** A steward's delegation of the steward role to a user, from a from-date to a to-date (both included). */
export type Delegation = { id: number; user: string; from: string; to: string; by: string };

export const delegationsKey = (sheetId: string) => ['sheet', sheetId, 'delegations'] as const;

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** The sheet's delegations and the form a steward delegates the steward role with. While the sheet's today is
 *  inside a delegation's dates its user acts as a steward. */
function Delegations({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const delegations = useQuery({
    queryKey: delegationsKey(sheetId),
    queryFn: () => getJson<Delegation[]>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`),
  }).data ?? [];
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { name: string; from: string; to: string }) =>
      postJson<Delegation>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`, { user, ...body }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: delegationsKey(sheetId) });
      setName('');
      setFrom('');
      setTo('');
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not delegate the steward role.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('Give the user name the steward role is delegated to.');
      return;
    }
    if (!DATE.test(from.trim()) || !DATE.test(to.trim())) {
      setError('The from- and to-dates must be dates (yyyy-mm-dd).');
      return;
    }
    if (to.trim() < from.trim()) {
      setError("The delegation's to-date must not be before its from-date.");
      return;
    }
    if (!once.start()) return;
    save.mutate({ name: name.trim(), from: from.trim(), to: to.trim() });
  };

  const change = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <section data-testid="delegations">
      <h2>Delegations</h2>
      <form data-testid="delegate-form" onSubmit={submit}>
        <input data-testid="delegate-user" placeholder="User name" value={name} onChange={change(setName)} />{' '}
        <input data-testid="delegate-from" placeholder="from yyyy-mm-dd" value={from} onChange={change(setFrom)} />{' '}
        <input data-testid="delegate-to" placeholder="to yyyy-mm-dd" value={to} onChange={change(setTo)} />{' '}
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate steward
        </button>
        {error && (
          <p data-testid="delegate-error" role="alert">
            {error}
          </p>
        )}
      </form>
      <p>Only a steward can delegate. The user acts as a steward while the sheet's today is inside the dates.</p>
      <ul>
        {delegations.map((d) => (
          <li key={d.id} data-testid="delegate-line">
            {d.user} acts as steward for {d.by} from {d.from} to {d.to}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 9, Component: Delegations });
