import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';

function FlagHead() {
  return (
    <>
      <th>Flag</th>
      <th />
    </>
  );
}

// The reason a person is flagged with while the flag is in force (nothing otherwise), and a reason
// and an until-date (blank: the sheet's today) with a button that flags them. Only a steward may
// flag; while a flag is in force only a steward may open a claim of theirs.
function PersonFlag({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const [error, setError] = useRowError(person.id, 'flag');
  const once = useOnce();
  const flag = useMutation({
    mutationFn: (body: { reason: string; until: string }) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/flag`,
        body,
        user,
      ),
    onSuccess: () => {
      setReason('');
      setUntil('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('That person could not be flagged.'),
  });

  const submit = () => {
    setError('');
    once((done) => flag.mutate({ reason: reason.trim(), until: until.trim() }, { onSettled: done }));
  };

  return (
    <>
      <td data-testid="person-flag-shown">{person.flag}</td>
      <td>
        <input
          data-testid="person-flag-reason"
          aria-label="Flag reason"
          placeholder="Reason"
          value={reason}
          onChange={(e) => {
            setReason(e.target.value);
            setError('');
          }}
        />{' '}
        <input
          type="date"
          data-testid="person-flag-until"
          aria-label="Flag until"
          value={until}
          onChange={(e) => {
            setUntil(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="button" data-testid="person-flag" onClick={submit} disabled={flag.isPending}>
          Flag
        </button>
        {error && (
          <p role="alert" data-testid="person-error">
            {error}
          </p>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 52, Component: FlagHead }),
  PersonRow.fill({ order: 52, Component: PersonFlag }),
];
