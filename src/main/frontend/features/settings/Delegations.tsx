import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { DATE, settingsKey, useSettings } from './settings';
import type { Settings } from './settings';
import { useOneAtATime } from '../../query/useOneAtATime';

type DelegationForm = { user: string; from: string; to: string };

const EMPTY: DelegationForm = { user: '', from: '', to: '' };

// The steward role delegated on the sheet and the form a steward delegates it with (blank dates: the sheet's today).
export function Delegations() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const delegations = useSettings().data?.delegations ?? [];
  const [form, setForm] = useState<DelegationForm>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (input: DelegationForm) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/delegations?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setForm(EMPTY);
      setError('');
      queryClient.setQueryData(settingsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet) });
    },
    onError: () => setError('That delegation was refused: only a steward delegates, and the to-date is not before the from-date.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const name = form.user.trim();
    const from = form.from.trim();
    const to = form.to.trim();
    if (name === '') {
      setError('Type a user name.');
      return;
    }
    if ((from !== '' && !DATE.test(from)) || (to !== '' && !DATE.test(to))) {
      setError('Type the dates as yyyy-mm-dd, or leave them blank for the sheet\'s today.');
      return;
    }
    if (from !== '' && to !== '' && to < from) {
      setError('The to-date cannot be before the from-date.');
      return;
    }
    once(() => save.mutateAsync({ user: name, from, to }));
  };

  const change = (patch: Partial<DelegationForm>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  return (
    <section data-testid="delegations">
      <ul data-testid="delegate-list">
        {delegations.map((d, i) => (
          <li key={i} data-testid="delegate-line">
            {d.by} delegated steward to {d.user} from {d.from} to {d.to}
          </li>
        ))}
      </ul>
      <form onSubmit={submit} data-testid="delegate-form">
        <input
          data-testid="delegate-user"
          placeholder="User name"
          value={form.user}
          onChange={(e) => change({ user: e.target.value })}
        />
        <input
          data-testid="delegate-from"
          placeholder="From (yyyy-mm-dd; blank: today)"
          value={form.from}
          onChange={(e) => change({ from: e.target.value })}
        />
        <input
          data-testid="delegate-to"
          placeholder="To (yyyy-mm-dd; blank: today)"
          value={form.to}
          onChange={(e) => change({ to: e.target.value })}
        />
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate steward
        </button>
        {error && <span data-testid="delegate-error">{error}</span>}
      </form>
    </section>
  );
}
