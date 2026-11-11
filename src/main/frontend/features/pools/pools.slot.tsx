import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { useUser } from '../session/session';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { trimmedSumDue } from '../rates/rates';
import { cutTotal, poolsKey, usePools } from './pools';
import type { Pool } from './pools';

// The sheet's pools: a name entry and an add button, each pool listed below with its total share, whether
// it is balanced (the shares total exactly 100.00; a retired member's counts as given) and its members, each
// with their share and their cut of the pot (blank when the pool is unbalanced, there is no pot or every member is retired, or the member's
// optional from- and to-dates do not include the sheet's today: such a member is left out of the total and balance too). A member is added from the pool's row: a person,
// or another pool (pool:<name>), and a share. A member pool's pot is its cut in the pool above. The server decides
// what is refused — a taken or blank name, a person already in the pool, a share not more than 0.00 and at most
// 100.00 with two decimals, a to-date before the from-date, a pool already in a pool, one containing itself through any chain, or a chain more than
// three deep — and the form shows its error. Each crew is listed as a pool too, under its name (no pool may take a
// crew's name), its members kept by the crew: it has no member form and is offered as no pool's member.
function Pools({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const pools = usePools(sheetId);
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { name: string }) =>
      postJson<Pool>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
    onError: () => setError('That pool could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once((done) => add.mutate({ name }, { onSettled: done }));
  };

  return (
    <section data-testid="pools">
      <h2>Pools</h2>
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input
            data-testid="pool-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {error && (
          <p role="alert" data-testid="pool-form-error">
            {error}
          </p>
        )}
      </form>
      <ul data-testid="pool-list">
        {(pools.data ?? []).map((p) => (
          <PoolRow key={p.name} sheetId={sheetId} pool={p} />
        ))}
      </ul>
    </section>
  );
}

function PoolRow({ sheetId, pool }: { sheetId: string; pool: Pool }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId);
  const pools = usePools(sheetId);
  // A person's id, or pool:<name> for a member pool.
  const [member, setMember] = useState('');
  const [share, setShare] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { pool: string; personId: string; memberPool: string; share: string; from: string; to: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/pool-members`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
    onError: () => setError(`That member could not be added to ${pool.name}.`),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const memberPool = member.startsWith('pool:') ? member.slice('pool:'.length) : '';
    const personId = memberPool ? '' : member;
    once((done) => add.mutate({ pool: pool.name, personId, memberPool, share, from, to }, { onSettled: done }));
  };

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong> — total share <span data-testid="pool-total-share">{pool.totalShare}</span>,
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li
            key={m.memberPool ? `pool-${m.memberPool}` : m.personId}
            data-testid={`pool-member-${pool.name}-${m.memberPool ? `pool-${m.memberPool}` : m.personId}`}
          >
            {m.name}
            {m.retired ? ' (retired)' : ''}: <span data-testid="pool-share">{m.share}</span>%, cut{' '}
            <span data-testid="pool-cut">{m.cut}</span>
            {m.periodCuts.map((c) => (
              <span key={c.period}>
                , {c.period}{' '}
                <span data-testid={`pool-cut-${c.period}`}>{c.cut}</span>
              </span>
            ))}
            {m.from || m.to ? ` (${m.from || 'open'} to ${m.to || 'open'}${m.active ? '' : ', not active'})` : ''}
          </li>
        ))}
      </ul>
      {!pool.crew && (
        <form onSubmit={submit}>
          <select
            data-testid="pool-member-select"
            value={member}
            onChange={(e) => {
              setMember(e.target.value);
              setError('');
            }}
          >
            <option value="">Person…</option>
            {(people.data ?? []).map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
            {(pools.data ?? [])
              .filter((p) => p.name !== pool.name && !p.crew)
              .map((p) => (
                <option key={`pool:${p.name}`} value={`pool:${p.name}`}>
                  {`pool:${p.name}`}
                </option>
              ))}
          </select>{' '}
          <input
            data-testid="pool-member-share"
            placeholder="share %"
            value={share}
            onChange={(e) => {
              setShare(e.target.value);
              setError('');
            }}
          />{' '}
          <input
            type="date"
            data-testid="pool-member-from"
            aria-label="Member from"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              setError('');
            }}
          />{' '}
          <input
            type="date"
            data-testid="pool-member-to"
            aria-label="Member to"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
              setError('');
            }}
          />{' '}
          <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
            Add member
          </button>
          {error && (
            <p role="alert" data-testid="pool-error">
              {error}
            </p>
          )}
        </form>
      )}
    </li>
  );
}

function CutHead() {
  return <th>Cut</th>;
}

function PersonCutTotal({ sheetId, person }: { sheetId: string; person: Person }) {
  const pools = usePools(sheetId).data ?? [];
  return <td data-testid="person-cut-total">{cutTotal(pools, person.id)}</td>;
}

// The sheet's cut total: everyone's cut total added up, trimmed of the highest and lowest.
function SheetCutTotal({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const pools = usePools(sheetId).data ?? [];
  return (
    <p>
      Cut in all: <strong data-testid="rollup-cut-total">{trimmedSumDue(people.map((p) => cutTotal(pools, p.id)))}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: -5.3, Component: Pools }),
  SheetBody.fill({ order: 20.8, Component: SheetCutTotal }),
  PersonHead.fill({ order: 22, Component: CutHead }),
  PersonRow.fill({ order: 22, Component: PersonCutTotal }),
];
