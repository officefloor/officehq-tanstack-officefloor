import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';
import { sheetKey } from '../sheet/useSheet';

/**
 * Transferring a person to another sheet: that sheet's id and the button to transfer. Only a steward
 * may; the server refuses a retired person, this sheet's own id, and a name, alias or contact already
 * on the other sheet. The person arrives there with their grades, terms, contact, aliases and claims,
 * and leaves this list.
 */
function PersonTransfer({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const [to, setTo] = useState('');
  const transfer = useMutation({
    mutationFn: (toSheet: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/transfer`, { to: toSheet, user }),
    // Everything else on both sheets first (the claims move with the person), then the list the row leaves.
    onSuccess: (_data, toSheet) => {
      void queryClient.invalidateQueries({ queryKey: sheetKey(toSheet.trim()) });
      return queryClient.invalidateQueries({
        queryKey: sheetKey(sheet),
        predicate: (q) => q.queryKey[2] !== peopleKey(sheet)[2],
      });
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  const actionTaken = useRowAction(sheet, person.id, transfer.reset);
  return (
    <td>
      <input
        data-testid="person-transfer-to"
        placeholder="Sheet id"
        value={to}
        onChange={(e) => {
          transfer.reset();
          setTo(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-transfer"
        disabled={transfer.isPending}
        onClick={() => {
          actionTaken();
          transfer.reset();
          transfer.mutate(to);
        }}
      >
        Transfer
      </button>
      {transfer.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          Could not transfer this person (a steward only, to another sheet, not retired, no contact
          already there, names matching at most one person there and not a retired one).
        </span>
      )}
    </td>
  );
}

/** The sheet a transferred person came from; nothing for anyone else. */
function PersonOrigin({ person }: { user: string; sheet: string; person: Person }) {
  return <td data-testid="person-origin">{person.origin ?? ''}</td>;
}

export const contributions = [
  PersonColumns.fill({ order: 45.5, Component: () => <th>From</th> }),
  PersonCells.fill({ order: 45.5, Component: PersonOrigin }),
  PersonColumns.fill({ order: 46, Component: () => <th>Transfer</th> }),
  PersonCells.fill({ order: 46, Component: PersonTransfer }),
];
