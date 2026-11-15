import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { listPeople, peopleKey } from '../people/api';
import type { Person } from '../people/api';
import { addPool, addPoolMember, listPools, poolsKey } from './api';
import type { Pool } from './api';

/** The sheet's pools and a form to add one (name trimmed; a blank or repeated one refused). Each pool
 *  lists its members with their shares, its total share (a retired member's counting as 0.00) and
 *  whether it is balanced (exactly 100.00), each member's cut of
 *  the sheet's pot (blank when unbalanced or no pot), and adds a member from its row. */
export function Pools({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const pools = useQuery({ queryKey: poolsKey(sheetId), queryFn: () => listPools(sheetId) });
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const [name, setName] = useState('');

  const save = useMutation({
    mutationFn: (input: string) => addPool(sheetId, user, input),
    onSuccess: (data) => {
      setName('');
      queryClient.setQueryData(poolsKey(sheetId), data);
    },
  });

  const submitOnce = useSubmitOnce();

  return (
    <section>
      <h3>Pools</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = name;
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(input);
          });
        }}
      >
        <input data-testid="pool-form-name" placeholder="Pool name" value={name}
          onChange={(e) => {
            setName(e.target.value);
            save.reset();
          }} />{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={save.isPending}>
          Add pool
        </button>
        {save.isError && (
          <span data-testid="pool-form-error">A pool needs a name not already used on the sheet.</span>
        )}
      </form>
      <ul>
        {(pools.data ?? []).map((p) => (
          <PoolRow key={p.id} sheetId={sheetId} user={user} pool={p} people={people.data ?? []} />
        ))}
      </ul>
    </section>
  );
}

function PoolRow({ sheetId, user, pool, people }: { sheetId: string; user: string; pool: Pool; people: Person[] }) {
  const queryClient = useQueryClient();
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');

  const add = useMutation({
    mutationFn: (input: { personId: number; share: string }) =>
      addPoolMember(sheetId, user, pool.id, input.personId, input.share),
    onSuccess: (data) => {
      setPersonId('');
      setShare('');
      queryClient.setQueryData(poolsKey(sheetId), data);
    },
  });

  const submitOnce = useSubmitOnce();

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong>{' '}
      total <span data-testid="pool-total-share">{pool.totalShare}</span>{' '}
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
            {m.name}{m.retired ? ' (retired)' : ''} share <span data-testid="pool-share">{m.share}</span>{' '}
            cut <span data-testid="pool-cut">{m.cut}</span>
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = { personId: Number(personId), share };
          submitOnce(() => {
            add.reset();
            return add.mutateAsync(input);
          });
        }}
      >
        <select data-testid="pool-member-select" value={personId}
          onChange={(e) => {
            setPersonId(e.target.value);
            add.reset();
          }}>
          <option value="">Person…</option>
          {people.map((person) => (
            <option key={person.id} value={String(person.id)}>{person.name}</option>
          ))}
        </select>{' '}
        <input data-testid="pool-member-share" placeholder="Share %" value={share}
          onChange={(e) => {
            setShare(e.target.value);
            add.reset();
          }} />{' '}
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
        {add.isError && (
          <span data-testid="pool-error">The member could not be added: a person not already in the pool, with a share more than 0.00 and at most 100.00 (two decimals).</span>
        )}
      </form>
    </li>
  );
}
