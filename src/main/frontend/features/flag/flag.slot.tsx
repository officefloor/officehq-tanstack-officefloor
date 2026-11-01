import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';

/**
 * A person's flag: its reason while in force (the sheet's today on or before its until-date), and the
 * reason, until-date and button for a steward to flag them (replacing any flag they had). While the
 * flag is in force only a steward may open a claim of theirs; after its date it shows nothing.
 */
function PersonFlag({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const flag = useMutation({
    mutationFn: (f: { reason: string; until: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/flag`, { ...f, user }),
    onSuccess: () => {
      setReason('');
      setUntil('');
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  const actionTaken = useRowAction(sheet, person.id, flag.reset);
  return (
    <>
      <td data-testid="person-flag-shown">{person.flag ?? ''}</td>
      <td>
        <input
          type="text"
          placeholder="flag reason"
          data-testid="person-flag-reason"
          value={reason}
          onChange={(e) => {
            setReason(e.target.value);
            flag.reset();
          }}
        />{' '}
        <input
          type="text"
          placeholder="until yyyy-mm-dd"
          data-testid="person-flag-until"
          value={until}
          onChange={(e) => {
            setUntil(e.target.value);
            flag.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-flag"
          disabled={flag.isPending}
          onClick={() => {
            actionTaken();
            flag.mutate({ reason, until });
          }}
        >
          Flag
        </button>
        {flag.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not flag this person.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 36.5,
    Component: () => (
      <>
        <th>Flag</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 36.5, Component: PersonFlag }),
];
