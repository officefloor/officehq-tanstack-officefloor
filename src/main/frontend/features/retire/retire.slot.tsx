import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { peopleKey, type Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';

function RetireHead() {
  return (
    <>
      <th>Retired</th>
      <th>Retired from</th>
      <th>Past retirements</th>
      <th />
    </>
  );
}

/** Whether the person is retired and from when, and the date and button that retire them (a blank date: the sheet's
 *  today). A person with a retirement date counts as retired at once, whatever the date; no claim of theirs may be
 *  dated after it. A retired person can be unretired (on the sheet's today): the retirement is then kept as a past one,
 *  one line each, and no claim of theirs may be dated after its from and before its to. A refused retirement or
 *  unretirement (anyone not retired) shows the row's error. */
function PersonRetire({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'retire');
  const unretireAction = usePersonRowAction(sheetId, person.id, 'unretire');
  const retire = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/retire`, { from: date, by: user }),
    onSuccess: () => {
      setFrom('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onSettled: submit.end,
  });
  const unretire = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/unretire`, { by: user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
    onSettled: submit.end,
  });

  return (
    <>
      <td data-testid="person-retired">{person.retiredFrom !== null ? 'yes' : 'no'}</td>
      <td data-testid="person-retired-from">{person.retiredFrom ?? ''}</td>
      <td>
        {(person.retirements ?? []).map((r, i) => (
          <div key={i} data-testid="person-retirement-line">
            {r.from} to {r.to}
          </div>
        ))}
      </td>
      <td>
        <input
          data-testid="person-retire-date"
          placeholder="yyyy-MM-dd"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            retire.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-retire"
          disabled={retire.isPending}
          onClick={() => {
            if (!submit.begin()) return;
            rowAction.take();
            retire.reset();
            retire.mutate(from.trim());
          }}
        >
          Retire
        </button>{' '}
        <button
          type="button"
          data-testid="person-unretire"
          disabled={unretire.isPending}
          onClick={() => {
            if (!submit.begin()) return;
            unretireAction.take();
            unretire.reset();
            unretire.mutate();
          }}
        >
          Unretire
        </button>
        {retire.isError && rowAction.isLatest && <span data-testid="person-error"> Could not retire this person.</span>}
        {unretire.isError && unretireAction.isLatest && (
          <span data-testid="person-error"> Could not unretire this person: they are not retired.</span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 40, Component: RetireHead }),
  PersonRow.fill({ order: 40, Component: PersonRetire }),
];
