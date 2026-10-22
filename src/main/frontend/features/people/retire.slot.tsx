import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { useSheetToday } from '../sheet/sheet';
import { peopleKey } from './people';
import type { Person } from './people';

/** Whether the person is retired and from when, and the form that retires them from a date (blank: the
 *  sheet's today). A claim dated after it is then refused; their claims keep counting in every total. */
function PersonRetire({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const today = useSheetToday(sheetId);
  const [from, setFrom] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const retire = useMutation({
    mutationFn: (body: { user: string; from: string }) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/retire`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setFrom('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not retire ${person.name}: the date must be yyyy-mm-dd.`),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!once.start()) return;
    retire.mutate({ user, from: from.trim() });
  };

  return (
    <td>
      Retired <span data-testid="person-retired">{person.retiredFrom !== null ? 'yes' : 'no'}</span>{' '}
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
      </form>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 40, Component: PersonRetire });
