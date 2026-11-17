import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { usePeople, type Person } from '../people/people';
import { personCutTotal, poolsKey, sheetCutTotal, usePools, type Pool, type PoolMember } from './pools';

/** One pool: its total share and whether it is balanced, each member's line with their share, and the picker that adds
 *  a person, or another pool (pool:<name>), with a share. A pool already a member of a pool, one that would contain
 *  itself through any chain, or a chain of pools more than three deep is refused too. A person already in the pool, or a share not more than 0.00 and at most 100.00 with two
 *  decimals, or a to-date before the from-date (both optional), is refused with the pool's error. */
function PoolRow({ sheetId, user, pool, pools }: { sheetId: string; user: string; pool: Pool; pools: Pool[] }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState('');
  const [share, setShare] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (m: { personId: number | null; memberPool: string | null; share: string; from: string; to: string }) =>
      postJson<PoolMember>(`/api/sheets/${encodeURIComponent(sheetId)}/pools/members`, { pool: pool.name, ...m, by: user }),
    onSuccess: async () => {
      setChosen('');
      setShare('');
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong> — total <span data-testid="pool-total-share">{pool.totalShare.toFixed(2)}</span>, balanced{' '}
      <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li
            key={m.pool === null ? m.personId : `pool-${m.pool}`}
            data-testid={m.pool === null ? `pool-member-${pool.name}-${m.personId}` : `pool-member-${pool.name}-pool-${m.pool}`}
          >
            {m.pool === null ? m.name : `pool:${m.pool}`}: <span data-testid="pool-share">{m.share.toFixed(2)}</span>, cut{' '}
            <span data-testid="pool-cut">{m.cut ?? ''}</span>
            {m.history.map((h) => (
              <span key={h.period}>
                {' '}
                {h.period}: <span data-testid={`pool-cut-${h.period}`}>{h.cut ?? ''}</span>
              </span>
            ))}
            {(m.from !== null || m.to !== null) && ` (${m.from ?? ''} – ${m.to ?? ''})`}
            {m.retired && ' (retired)'}
            {!m.active && ' (not in force)'}
          </li>
        ))}
      </ul>
      <select
        data-testid="pool-member-select"
        value={chosen}
        onChange={(e) => {
          setChosen(e.target.value);
          add.reset();
        }}
      >
        <option value="">Person…</option>
        {(people.data ?? []).map((p) => (
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
      <input
        data-testid="pool-member-share"
        placeholder="Share %"
        value={share}
        onChange={(e) => {
          setShare(e.target.value);
          add.reset();
        }}
      />{' '}
      <input
        data-testid="pool-member-from"
        placeholder="from yyyy-MM-dd"
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          add.reset();
        }}
      />{' '}
      <input
        data-testid="pool-member-to"
        placeholder="to yyyy-MM-dd"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          add.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="pool-member-add"
        disabled={add.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          add.reset();
          // nothing chosen: no person 0, so the server refuses it
          const dates = { from: from.trim(), to: to.trim() };
          if (chosen.startsWith('pool:')) add.mutate({ personId: null, memberPool: chosen.slice('pool:'.length), share, ...dates });
          else add.mutate({ personId: Number(chosen), memberPool: null, share, ...dates });
        }}
      >
        Add member
      </button>
      {add.isError && <span data-testid="pool-error"> Could not add this member.</span>}
    </li>
  );
}

/** The sheet's pools: the form that adds one (its name trimmed, not blank, not already taken), and each pool's row. */
function Pools({ sheetId, user }: { sheetId: string; user: string }) {
  const pools = usePools(sheetId);
  const queryClient = useQueryClient();
  const [name, setName] = useState('');

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (n: string) => postJson<Pool>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`, { name: n, by: user }),
    onSuccess: async () => {
      setName('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <section data-testid="pools">
      <h3>Pools</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          add.reset();
          add.mutate(name);
        }}
      >
        <input
          data-testid="pool-form-name"
          placeholder="Pool name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {add.isError && <span data-testid="pool-form-error"> A pool's name must not be blank nor already taken.</span>}
      </form>
      {pools.isError && <p data-testid="pools-load-error">Could not load pools.</p>}
      <ul>
        {(pools.data ?? []).map((p) => (
          <PoolRow key={p.name} sheetId={sheetId} user={user} pool={p} pools={pools.data ?? []} />
        ))}
      </ul>
    </section>
  );
}

function CutHead() {
  return <th>Cut total</th>;
}

/** A person's cut total across the pools they are a member of. */
function PersonCutTotal({ sheetId, person }: { sheetId: string; person: Person }) {
  const pools = usePools(sheetId);
  return <td data-testid="person-cut-total">{personCutTotal(pools.data, person.id)}</td>;
}

/** The sheet's cut total: everyone's cut total added up, trimmed (see sheetCutTotal). */
function CutTotal({ sheetId }: { sheetId: string }) {
  const pools = usePools(sheetId);
  const people = usePeople(sheetId);
  return (
    <p>
      Cut total <span data-testid="rollup-cut-total">{sheetCutTotal(pools.data, (people.data ?? []).map((p) => p.id))}</span>
    </p>
  );
}

export const contributions = [
  SheetPage.fill({ order: 9.7, Component: Pools }),
  PeopleHead.fill({ order: 62, Component: CutHead }),
  PersonRow.fill({ order: 62, Component: PersonCutTotal }),
  SheetPage.fill({ order: 36, Component: CutTotal }),
];
