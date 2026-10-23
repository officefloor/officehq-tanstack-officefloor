import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { poolsKey } from '../pools/pools';
import { useChangeToday } from '../sheet/sheet';
import { peopleKey } from './people';
import { usePersonError } from './rowError';
import type { Person } from './people';
import { useColumnShown } from './columns';

/** Whether the person is retired and from when, their past retirements, the form that retires them from a date
 *  (blank: the sheet's today) and the button that unretires them on the sheet's today. A claim dated after the
 *  retirement date, or after the from and before the to of a past retirement, is refused. */
function PersonRetire({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const showRetired = useColumnShown('retired');
  const queryClient = useQueryClient();
  const today = useChangeToday(sheetId);
  const [from, setFrom] = useState('');
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const retire = useMutation({
    mutationFn: (body: { user: string; from: string }) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/retire`, body),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]);
      setFrom('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not retire ${person.name}: the date must be yyyy-mm-dd.`),
    onSettled: once.done,
  });

  const unretire = useMutation({
    mutationFn: (body: { user: string }) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/unretire`, body),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]),
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : `Could not unretire ${person.name}.`),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    begin();
    if (!once.start()) return;
    retire.mutate({ user, from: from.trim() });
  };

  return (
    <td>
      {showRetired && (
        <>
          Retired <span data-testid="person-retired">{person.retiredFrom !== null ? 'yes' : 'no'}</span>{' '}
        </>
      )}
      from <span data-testid="person-retired-from">{person.retiredFrom ?? ''}</span>{' '}
      <form onSubmit={submit} style={{ display: 'inline' }}>
        <input
          data-testid="person-retire-date"
          placeholder={today || 'yyyy-mm-dd'}
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="submit" data-testid="person-retire" disabled={retire.isPending}>
          Retire
        </button>
      </form>{' '}
      <button
        type="button"
        data-testid="person-unretire"
        disabled={unretire.isPending}
        onClick={() => {
          begin();
          if (!once.start()) return;
          unretire.mutate({ user });
        }}
      >
        Unretire
      </button>
      <ul>
        {person.retirements.map((r, i) => (
          <li key={i} data-testid="person-retirement-line">
            {r.from} to {r.to}
          </li>
        ))}
      </ul>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 40, Component: PersonRetire });
