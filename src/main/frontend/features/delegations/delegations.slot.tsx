import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { delegationsKey, delegationsPath, useDelegations } from './delegations';
import type { Delegation } from './delegations';

type Draft = { user: string; from: string; to: string };
const EMPTY: Draft = { user: '', from: '', to: '' };

// The delegations made on the sheet, and a form by which a steward lends the steward role to a user from a from-date
// (blank: the sheet's today) to a to-date, both inclusive.
function Delegations({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: delegations = [] } = useDelegations(sheet);
  const [draft, setDraft] = useState<Draft>(EMPTY);

  const save = useMutation({
    mutationFn: (body: Draft) => postJson<Delegation[]>(byUser(delegationsPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(delegationsKey(sheet), updated);
      setDraft(EMPTY);
    },
  });
  const send = useSubmitOnce(save);

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses anyone but a steward, a blank user, and a to-date before the from-date.
    send(draft);
  };

  return (
    <section>
      <h2>Delegations</h2>
      <form onSubmit={submit}>
        <label>
          User{' '}
          <input data-testid="delegate-user" value={draft.user} onChange={(e) => change({ user: e.target.value })} />
        </label>{' '}
        <label>
          From{' '}
          <input
            data-testid="delegate-from"
            placeholder="yyyy-mm-dd (blank: today)"
            value={draft.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="delegate-to"
            placeholder="yyyy-mm-dd"
            value={draft.to}
            onChange={(e) => change({ to: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate steward
        </button>
        {save.isError && (
          <span data-testid="delegate-error" role="alert">
            {' '}Only a steward delegates, to a user, with a to-date on or after the from-date.
          </span>
        )}
      </form>
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

export const contribution = SheetPanels.fill({ order: 8, Component: Delegations });
