import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, peoplePath, personActionKey } from './people';
import type { Person } from './people';

function FlagHeader() {
  return (
    <>
      <th>Flag</th>
      <th />
    </>
  );
}

// The reason of the person's flag while it is in force (the sheet's today on or before its until-date; nothing after
// it), and the reason, until-date and button with which a steward flags them (a blank until-date is the sheet's today).
// While flagged, only a steward may open a claim of theirs.
function PersonFlag({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const flag = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (body: { reason: string; until: string }) =>
      postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/flag`, user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(peopleKey(sheet), updated);
      setReason('');
      setUntil('');
    },
  });
  const act = useSubmitOnce(flag);
  const latest = useIsLatest(personActionKey(sheet, person.id), flag.submittedAt);

  return (
    <>
      <td data-testid="person-flag-shown">{person.flagged ? (person.flagReason ?? '') : ''}</td>
      <td>
        <input
          data-testid="person-flag-reason"
          type="text"
          placeholder="reason"
          value={reason}
          onChange={(e) => {
            setReason(e.target.value);
            flag.reset();
          }}
        />{' '}
        <input
          data-testid="person-flag-until"
          type="text"
          placeholder="until yyyy-mm-dd"
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
          onClick={() => act({ reason: reason.trim(), until: until.trim() })}
        >
          Flag
        </button>
        {flag.isError && latest && (
          <span data-testid="person-error" role="alert">
            {' '}That flag was refused.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 42, Component: FlagHeader }),
  PersonColumns.fill({ order: 42, Component: PersonFlag }),
];
