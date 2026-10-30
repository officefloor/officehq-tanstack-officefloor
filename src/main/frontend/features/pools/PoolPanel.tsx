import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { usePeople, type Person } from '../people/usePeople';
import { poolsKey, usePools, type Pool } from './usePools';

// The sheet's pools and the form that adds one (a blank name, or one already a pool, is refused). Each
// pool lists its members with their shares, its total share (a retired member counting 0.00) and whether
// it is balanced (the total is exactly 100.00), and adds a member from its row: a person already in the
// pool, or a share not more than 0.00 and at most 100.00 with two decimals, is refused.
export function PoolPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const pools = usePools(sheet);
  const people = usePeople(sheet);
  const [name, setName] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { name: string }) =>
      postJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheet)}/pools`, { ...body, user }),
    onSuccess: (saved) => {
      setName('');
      queryClient.setQueryData(poolsKey(sheet), saved);
    },
  });
  return (
    <>
      <ul data-testid="pool-list">
        {pools.map((p) => (
          <PoolRow key={p.name} pool={p} people={people} sheet={sheet} user={user} />
        ))}
      </ul>
      <form
        data-testid="pool-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ name: name.trim() });
          });
        }}
      >
        <input
          data-testid="pool-form-name"
          value={name}
          placeholder="Pool name"
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="pool-form-submit">Add pool</button>
        {add.isError && <span data-testid="pool-form-error" role="alert"> Could not add the pool: its name must not be blank or already a pool.</span>}
      </form>
    </>
  );
}

function PoolRow({ pool, people, sheet, user }: { pool: Pool; people: Person[]; sheet: string; user: string }) {
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { personId: string; share: string }) =>
      postJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheet)}/pool-members`, { ...body, pool: pool.name, user }),
    onSuccess: (saved) => {
      setPersonId('');
      setShare('');
      queryClient.setQueryData(poolsKey(sheet), saved);
    },
  });
  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <span data-testid="pool-name">{pool.name}</span>{' '}
      total <span data-testid="pool-total-share">{pool.totalShare}</span>{' '}
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
            <span data-testid="pool-member-name">{m.name}</span>{' '}
            <span data-testid="pool-share">{m.share}</span>
            {m.retired && ' (retired)'}
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ personId, share: share.trim() });
          });
        }}
      >
        <select
          data-testid="pool-member-select"
          value={personId}
          onChange={(e) => {
            setPersonId(e.target.value);
            add.reset();
          }}
        >
          <option value="">Choose a person</option>
          {people.map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>{' '}
        <input
          data-testid="pool-member-share"
          value={share}
          placeholder="Share %"
          onChange={(e) => {
            setShare(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="pool-member-add">Add member</button>
        {add.isError && <span data-testid="pool-error" role="alert"> Could not add the member: already in the pool, or the share is not more than 0.00 and at most 100.00 with two decimals.</span>}
      </form>
    </li>
  );
}
