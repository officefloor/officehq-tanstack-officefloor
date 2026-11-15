import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { listPeople, peopleKey } from '../people/api';
import type { Person } from '../people/api';
import { downloadCsv } from '../people/ExportPeople';
import { addPool, addPoolMember, listPools, poolsKey } from './api';
import type { MemberRef, Pool } from './api';

/** The sheet's pools and a form to add one (name trimmed; a blank or repeated one refused). Each pool
 *  lists its members with their shares, its total share (a retired member's counting as given) and
 *  whether it is balanced (exactly 100.00), each member's cut of
 *  the pool's pot (blank when unbalanced or no pot), and adds a member — a person or another pool, listed
 *  as pool:<name> — from its row, with optional from/to dates (a member whose dates leave out the sheet's
 *  today counts for nothing and has no cut). A pool that is a member of another cuts its cut there. */
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
      <button type="button" data-testid="export-pools"
        onClick={() => {
          void listPools(sheetId).then((fresh) => {
            queryClient.setQueryData(poolsKey(sheetId), fresh);
            downloadPools(fresh);
          });
        }}>
        Export pools
      </button>
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
          <PoolRow key={p.id} sheetId={sheetId} user={user} pool={p} people={people.data ?? []} pools={pools.data ?? []} />
        ))}
      </ul>
    </section>
  );
}

/** Downloads the pools as a CSV file: one line per member, pools and members in the order added, a pool
 *  member as pool:<name> and its cut as shown (empty when none). */
function downloadPools(pools: Pool[]) {
  downloadCsv('pools.csv', [['pool', 'member', 'share', 'cut'],
    ...pools.flatMap((p) => p.members.map((m) => [p.name, m.poolId !== null ? `pool:${m.name}` : m.name, m.share, m.cut]))]);
}

/** The select's value for a member: a person's id, or pool:<id> for a pool. */
function memberRef(value: string): MemberRef {
  return value.startsWith('pool:') ? { memberPoolId: Number(value.slice(5)) } : { personId: Number(value) };
}

function PoolRow({ sheetId, user, pool, people, pools }: { sheetId: string; user: string; pool: Pool; people: Person[]; pools: Pool[] }) {
  const queryClient = useQueryClient();
  const [member, setMember] = useState('');
  const [share, setShare] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const add = useMutation({
    mutationFn: (input: { member: MemberRef; share: string; from: string; to: string }) =>
      addPoolMember(sheetId, user, pool.id, input.member, input.share, input.from, input.to),
    onSuccess: (data) => {
      setMember('');
      setShare('');
      setFrom('');
      setTo('');
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
          <li key={m.poolId !== null ? `pool-${m.poolId}` : m.personId}
            data-testid={`pool-member-${pool.name}-${m.poolId !== null ? `pool-${m.name}` : m.personId}`}>
            {m.poolId !== null ? `pool:${m.name}` : m.name}{m.retired ? ' (retired)' : ''} share <span data-testid="pool-share">{m.share}</span>{' '}
            {m.from || m.to ? `${m.from ?? '…'} to ${m.to ?? '…'}${m.current ? '' : ' (not current)'} ` : ''}
            cut <span data-testid="pool-cut">{m.cut}</span>
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = { member: memberRef(member), share, from, to };
          submitOnce(() => {
            add.reset();
            return add.mutateAsync(input);
          });
        }}
      >
        <select data-testid="pool-member-select" value={member}
          onChange={(e) => {
            setMember(e.target.value);
            add.reset();
          }}>
          <option value="">Member…</option>
          {people.map((person) => (
            <option key={person.id} value={String(person.id)}>{person.name}</option>
          ))}
          {pools.map((p) => (
            <option key={`pool-${p.id}`} value={`pool:${p.id}`}>{`pool:${p.name}`}</option>
          ))}
        </select>{' '}
        <input data-testid="pool-member-share" placeholder="Share %" value={share}
          onChange={(e) => {
            setShare(e.target.value);
            add.reset();
          }} />{' '}
        <input type="date" data-testid="pool-member-from" value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            add.reset();
          }} />{' '}
        <input type="date" data-testid="pool-member-to" value={to}
          onChange={(e) => {
            setTo(e.target.value);
            add.reset();
          }} />{' '}
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
        {add.isError && (
          <span data-testid="pool-error">The member could not be added: a person not already in the pool, or a pool not already in one that keeps chains acyclic and at most three deep, with a share more than 0.00 and at most 100.00 (two decimals), and a to-date not before the from-date.</span>
        )}
      </form>
    </li>
  );
}
