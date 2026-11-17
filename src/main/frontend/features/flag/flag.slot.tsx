import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { flagOn, peopleKey, type Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';
import { useSheetToday } from '../session/session';

function FlagHead() {
  return <th>Flag</th>;
}

/** The reason of the person's flag while it is in force (the sheet's today on or before its until-date; nothing after),
 *  and the reason, until-date (blank: the sheet's today) and button a steward flags them with. Only a steward may flag,
 *  and a flag needs a reason: a refused flag shows the row's error. */
function PersonFlagCell({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const today = useSheetToday(sheetId);
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'flag');
  const flag = useMutation({
    mutationFn: (body: { reason: string; until: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/flag`, { ...body, by: user }),
    onSuccess: () => {
      setReason('');
      setUntil('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <td>
      <span data-testid="person-flag-shown">{flagOn(person, today)}</span>{' '}
      <input
        data-testid="person-flag-reason"
        placeholder="reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          flag.reset();
        }}
      />{' '}
      <input
        data-testid="person-flag-until"
        placeholder="until yyyy-MM-dd"
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
          if (!submit.begin()) return;
          rowAction.take();
          flag.reset();
          flag.mutate({ reason: reason.trim(), until: until.trim() });
        }}
      >
        Flag
      </button>
      {flag.isError && rowAction.isLatest && <span data-testid="person-error"> Could not flag this person.</span>}
    </td>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 51, Component: FlagHead }),
  PersonRow.fill({ order: 51, Component: PersonFlagCell }),
];
