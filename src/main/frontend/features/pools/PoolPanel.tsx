import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { usePeople, type Person } from '../people/usePeople';
import { poolsKey, usePools, type Pool } from './usePools';

// The sheet's pools and the form that adds one (a blank name, or one already a pool, is refused). Each
// pool lists its members with their shares, its total share (a retired member's share counted as given) and whether
// it is balanced (the total is exactly 100.00), and adds a member from its row: a person already in the
// pool, or a share not more than 0.00 and at most 100.00 with two decimals, is refused. Each member of a balanced
// pool shows their cut of its pot (empty while the pool is unbalanced or has no pot). A retired or held member cuts 0.00, their share spread over
// the others in proportion to theirs; a pool whose every member is retired shows no cuts. A member may be another pool,
// chosen as pool:<name>: a pool is a member of at most one pool, never contains itself through any chain, and a chain
// of pools is at most three deep. A pool that is a member of none has the sheet's pot; a pool that is a member of
// another has its cut there as its pot. A membership may carry an optional from-date and to-date (a to-date before
// the from-date is refused); a member whose dates do not include the sheet's today is left out entirely: not in the
// total share, not in the balance, no cut.
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
          <PoolRow key={p.name} pool={p} pools={pools} people={people} sheet={sheet} user={user} />
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

function PoolRow({ pool, pools, people, sheet, user }: { pool: Pool; pools: Pool[]; people: Person[]; sheet: string; user: string }) {
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { personId?: string; memberPool?: string; share: string; from: string; to: string }) =>
      postJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheet)}/pool-members`, { ...body, pool: pool.name, user }),
    onSuccess: (saved) => {
      setPersonId('');
      setShare('');
      setFrom('');
      setTo('');
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
          <li
            key={m.memberPool ? `pool:${m.memberPool}` : m.personId}
            data-testid={m.memberPool ? `pool-member-${pool.name}-pool-${m.memberPool}` : `pool-member-${pool.name}-${m.personId}`}
          >
            <span data-testid="pool-member-name">{m.name}</span>{' '}
            <span data-testid="pool-share">{m.share}</span>{' '}
            cut <span data-testid="pool-cut">{m.cut}</span>
            {m.retired && ' (retired)'}
            {!m.current && ' (not current)'}
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            const memberPool = personId.startsWith('pool:') ? personId.slice('pool:'.length) : '';
            const dates = { share: share.trim(), from: from.trim(), to: to.trim() };
            return add.mutateAsync(memberPool ? { memberPool, ...dates } : { personId, ...dates });
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
          {pools.map((p) => (
            <option key={`pool:${p.name}`} value={`pool:${p.name}`}>{`pool:${p.name}`}</option>
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
        <input
          type="date"
          data-testid="pool-member-from"
          value={from}
          title="Member from (blank: unbounded)"
          onChange={(e) => {
            setFrom(e.target.value);
            add.reset();
          }}
        />{' '}
        <input
          type="date"
          data-testid="pool-member-to"
          value={to}
          title="Member to (blank: unbounded)"
          onChange={(e) => {
            setTo(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="pool-member-add">Add member</button>
        {add.isError && <span data-testid="pool-error" role="alert"> Could not add the member: already in the pool, a pool already in a pool, containing this pool, or nesting more than three deep, or the share is not more than 0.00 and at most 100.00 with two decimals, or the to-date is before the from-date.</span>}
      </form>
    </li>
  );
}
