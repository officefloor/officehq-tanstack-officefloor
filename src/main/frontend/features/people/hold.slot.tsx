import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { useChangeToday } from '../sheet/sheet';
import { poolsKey } from '../pools/pools';
import { peopleKey } from './people';
import { usePersonError } from './rowError';
import type { Person } from './people';
import { useColumnShown } from './columns';

/** Whether the person has any hold, their holds, and the form that adds one from a from-date (blank: the sheet's
 *  today) to an optional to-date (blank: open-ended). Only their OPEN claims dated within a hold, and not bound by
 *  a statement, count in no live total; a hold ends on its to-date. */
function PersonHold({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const showHold = useColumnShown('hold');
  const queryClient = useQueryClient();
  const today = useChangeToday(sheetId);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const hold = useMutation({
    mutationFn: (body: { user: string; from: string; to: string }) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/hold`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      // a hold in force on the sheet's today moves pool cuts
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      setFrom('');
      setTo('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not hold ${person.name}: the dates must be yyyy-mm-dd.`),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    begin();
    if (!once.start()) return;
    hold.mutate({ user, from: from.trim(), to: to.trim() });
  };

  const change = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <td>
      {showHold && (
        <>
          Hold <span data-testid="person-hold">{person.holds.length > 0 ? 'yes' : 'no'}</span>
        </>
      )}
      <ul>
        {person.holds.map((h, i) => (
          <li key={i} data-testid="person-hold-line">
            {h.from ?? 'start'} to {h.to ?? 'open'}
          </li>
        ))}
      </ul>
      <form onSubmit={submit} style={{ display: 'inline' }}>
        <input data-testid="person-hold-from" placeholder={today || 'yyyy-mm-dd'} value={from} onChange={change(setFrom)} />{' '}
        <input data-testid="person-hold-to" placeholder="to (optional)" value={to} onChange={change(setTo)} />{' '}
        <button type="submit" data-testid="person-hold-toggle" disabled={hold.isPending}>
          Hold
        </button>
      </form>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 45, Component: PersonHold });
