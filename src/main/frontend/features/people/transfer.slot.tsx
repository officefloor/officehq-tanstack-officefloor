import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, peoplePath, personActionKey } from './people';
import type { Person } from './people';

function TransferHeader() {
  return <th />;
}

// The id of the sheet to transfer this person to, and the button that transfers them. Only a steward may; the server
// refuses a retired person, this sheet's own id, or a contact already on the other sheet; a name or alias already there
// merges them into that person (refused when that person is retired or the names are on several people). A transfer
// drops the person's claims, patron links and pool memberships here, so everything on both sheets is re-read.
function PersonTransfer({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [to, setTo] = useState('');
  const transfer = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (target: string) =>
      postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/transfer`, user), { to: target }),
    onSuccess: async (updated, target) => {
      await queryClient.invalidateQueries({
        predicate: (q) =>
          (q.queryKey.includes(sheet) && q.queryKey[0] !== 'sheet' && q.queryKey[0] !== 'people') ||
          q.queryKey.includes(target),
      });
      queryClient.setQueryData(peopleKey(sheet), updated);
      setTo('');
    },
  });
  const act = useSubmitOnce(transfer);
  const latest = useIsLatest(personActionKey(sheet, person.id), transfer.submittedAt);

  return (
    <td>
      <input
        data-testid="person-transfer-to"
        placeholder="transfer to sheet…"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          transfer.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-transfer"
        disabled={transfer.isPending}
        onClick={() => act(to.trim())}
      >
        Transfer
      </button>
      {transfer.isError && latest && (
        <span data-testid="person-error" role="alert">
          {' '}Only a steward may transfer a person, not retired, to another sheet without their contact, merging only into one person who is not retired.
        </span>
      )}
    </td>
  );
}

// The button that undoes the transfer that brought this person here, sending them back to the sheet they came from by
// the transfer rules. The server refuses anyone but a steward, any day but the sheet's today they arrived, and a person
// who did not arrive by transfer; both sheets are re-read as for a transfer.
function PersonTransferUndo({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const undo = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: () => postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/transfer-undo`, user), {}),
    onSuccess: async (updated) => {
      const target = person.origin;
      await queryClient.invalidateQueries({
        predicate: (q) =>
          (q.queryKey.includes(sheet) && q.queryKey[0] !== 'sheet' && q.queryKey[0] !== 'people') ||
          (target != null && q.queryKey.includes(target)),
      });
      queryClient.setQueryData(peopleKey(sheet), updated);
    },
  });
  const act = useSubmitOnce(undo);
  const latest = useIsLatest(personActionKey(sheet, person.id), undo.submittedAt);

  return (
    <td>
      <button
        type="button"
        data-testid="person-transfer-undo"
        disabled={undo.isPending}
        onClick={() => act(undefined)}
      >
        Undo transfer
      </button>
      {undo.isError && latest && (
        <span data-testid="person-error" role="alert">
          {' '}Only a steward may undo a transfer, on the day the person arrived by it.
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 75, Component: TransferHeader }),
  PersonColumns.fill({ order: 75, Component: PersonTransfer }),
  PersonColumnHeaders.fill({ order: 76, Component: TransferHeader }),
  PersonColumns.fill({ order: 76, Component: PersonTransferUndo }),
];
