import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeople, type Person } from '../people/usePeople';
import { poolsKey, usePools, type Pool } from './usePools';

/** One pool: its members with their shares and cuts of the sheet's pot, its total share and whether it is balanced, and a form that
 *  adds a member (a person on the sheet, or another pool as pool:<name>, a share percent and an optional from-date and
 *  to-date: a member whose dates do not include the sheet's today is left out of the pool). */
function PoolRow({ sheetId, pool, pools, people }: { sheetId: string; pool: Pool; pools: Pool[]; people: Person[] }) {
  const queryClient = useQueryClient();
  // A person's id, or pool:<name> for another pool.
  const [member, setMember] = useState('');
  const [share, setShare] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const submitOnce = useSingleFlight();
  const add = useMutation({
    mutationFn: (f: { pool: string; personId: number | null; memberPool: string | null; share: string; from: string; to: string }) =>
      postJson<{ pools: Pool[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/pools/members`, f),
    onSuccess: async () => {
      setMember('');
      setShare('');
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
  });

  return (
    <div data-testid={`pool-row-${pool.name}`}>
      <strong data-testid="pool-name">{pool.name}</strong>{' '}
      Total <span data-testid="pool-total-share">{pool.totalShare}</span>{' '}
      Balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li
            key={m.pool === null ? m.personId : `pool:${m.pool}`}
            data-testid={m.pool === null ? `pool-member-${pool.name}-${m.personId}` : `pool-member-${pool.name}-pool-${m.pool}`}
          >
            <span data-testid="pool-member-name">{m.name}</span>{' '}
            <span data-testid="pool-share">{m.share}</span>{' '}
            <span data-testid="pool-cut">{m.cut ?? ''}</span>
            {(m.from !== null || m.to !== null) && ` (${m.from ?? ''} – ${m.to ?? ''})`}
            {m.retired && ' (retired)'}
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            const memberPool = member.startsWith('pool:') ? member.slice('pool:'.length) : null;
            const personId = member === '' || memberPool !== null ? null : Number(member);
            return add.mutateAsync({ pool: pool.name, personId, memberPool, share, from: from.trim(), to: to.trim() });
          });
        }}
      >
        <select
          data-testid="pool-member-select"
          value={member}
          onChange={(e: ChangeEvent<HTMLSelectElement>) => {
            setMember(e.target.value);
            add.reset();
          }}
        >
          <option value="">Choose a person or pool</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
          {pools.map((p) => (
            <option key={`pool:${p.name}`} value={`pool:${p.name}`}>
              {`pool:${p.name}`}
            </option>
          ))}
        </select>{' '}
        <label>
          Share{' '}
          <input
            data-testid="pool-member-share"
            inputMode="decimal"
            value={share}
            onChange={(e) => {
              setShare(e.target.value);
              add.reset();
            }}
          />
        </label>{' '}
        <input
          placeholder="from yyyy-mm-dd"
          data-testid="pool-member-from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            add.reset();
          }}
        />{' '}
        <input
          placeholder="to yyyy-mm-dd"
          data-testid="pool-member-to"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
      </form>
      {add.isError && <p data-testid="pool-error">{add.error.message}</p>}
    </div>
  );
}

/** The sheet's pools: a form that adds one (a name, unique on the sheet) and each pool with its members. */
function Pools({ sheetId }: { sheetId: string }) {
  const pools = usePools(sheetId);
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const [name, setName] = useState('');

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { name: string }) =>
      postJson<{ pools: Pool[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`, f),
    onSuccess: async () => {
      setName('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
  });

  return (
    <section data-testid="pools">
      <form
        data-testid="pool-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ name });
          });
        }}
      >
        <label>
          Pool{' '}
          <input
            data-testid="pool-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={save.isPending}>
          Add pool
        </button>
        {save.isError && <p data-testid="pool-form-error">{save.error.message}</p>}
      </form>
      {(pools.data ?? []).map((p) => (
        <PoolRow key={p.name} sheetId={sheetId} pool={p} pools={pools.data ?? []} people={people.data ?? []} />
      ))}
      {pools.isError && <p data-testid="pools-error">{pools.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7, Component: ({ sheetId }) => <Pools sheetId={sheetId} /> });
