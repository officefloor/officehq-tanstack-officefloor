import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople } from '../people/people';
import { poolsPath, poolsQueryKey, usePools, type Pool } from './pools';

// The sheet's pools, each with its members (with their cut of the pot when the pool is balanced), total share and whether it is balanced, and the entry that adds a pool.
// A blank name, or one already a pool's, is refused by the server and shows the error beside the entry, which goes
// as soon as the entry changes.
function Pools() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const pools = usePools();
  const [name, setName] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { name: string }) => postJson<Pool[]>(poolsPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsQueryKey(sheet ?? '') });
      setName('');
    },
  });
  const submit = useSubmitOnce(add);

  if (!user || !sheet) return null;
  return (
    <section data-testid="pools">
      <h2>Pools</h2>
      <table>
        <thead>
          <tr><th>Pool</th><th>Members</th><th>Total share</th><th>Balanced</th><th>Add member</th></tr>
        </thead>
        <tbody>
          {(pools.data ?? []).map((p) => <PoolRow key={p.id} pool={p} />)}
        </tbody>
      </table>
      <form
        data-testid="pool-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ name });
        }}
      >
        <input
          data-testid="pool-form-name"
          placeholder="Pool name"
          value={name}
          onChange={(e) => { setName(e.target.value); add.reset(); }}
        />
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {add.isError && (
          <span data-testid="pool-form-error">A pool's name must not be blank or already a pool's.</span>
        )}
      </form>
    </section>
  );
}

// A pool's row: its members with their shares, its total and balance, and the person select and share that add a
// member. A person already in the pool, or a share that is not more than 0.00 and at most 100.00 (two decimals),
// is refused and shows the error on the pool, which goes as soon as the entry changes.
function PoolRow({ pool }: { pool: Pool }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { personId: number | null; share: string }) =>
      postJson<Pool[]>(`${poolsPath(sheet ?? '')}/${pool.id}/members`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsQueryKey(sheet ?? '') });
      setShare('');
    },
  });
  const submit = useSubmitOnce(add);
  return (
    <tr data-testid={`pool-row-${pool.name}`}>
      <td data-testid="pool-name">{pool.name}</td>
      <td>
        <ul>
          {pool.members.map((m) => (
            <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
              <span data-testid="pool-member-name">{m.name}</span>{' '}
              <span data-testid="pool-share">{m.share}</span>{' '}
              <span data-testid="pool-cut">{m.cut}</span>
            </li>
          ))}
        </ul>
      </td>
      <td data-testid="pool-total-share">{pool.totalShare}</td>
      <td data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</td>
      <td>
        <select
          data-testid="pool-member-select"
          value={personId}
          onChange={(e) => { setPersonId(e.target.value); add.reset(); }}
        >
          <option value="">Choose a person</option>
          {(people.data ?? []).map((p) => (
            <option key={p.id} value={String(p.id)}>{p.name}</option>
          ))}
        </select>
        <input
          data-testid="pool-member-share"
          placeholder="Share %"
          value={share}
          onChange={(e) => { setShare(e.target.value); add.reset(); }}
        />
        <button
          type="button"
          data-testid="pool-member-add"
          disabled={add.isPending}
          onClick={() => submit({ personId: personId === '' ? null : Number(personId), share })}
        >
          Add member
        </button>
        {add.isError && (
          <span data-testid="pool-error">A member must not already be in the pool, with a share above 0.00 and at most 100.00.</span>
        )}
      </td>
    </tr>
  );
}

export const contribution = HomeMain.fill({ order: 6.7, Component: Pools });
