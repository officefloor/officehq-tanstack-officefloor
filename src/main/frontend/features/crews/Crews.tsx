import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople } from '../people/people';
import { crewDue, crewsKey, useCrews, useDueByPerson } from './crews';
import type { Crew } from './crews';
import { poolsKey, usePools } from '../pools/pools';
import { useOneAtATime } from '../../query/useOneAtATime';

// The sheet's crews, each with its lead and due-total, and the form that adds one; a blank name, one already a
// crew's or a pool's (a crew is also a pool), or no lead chosen, is refused.
export function Crews() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const crews = useCrews().data ?? [];
  const people = usePeople().data ?? [];
  const pools = usePools().data ?? [];
  const dueOf = useDueByPerson();
  const [name, setName] = useState('');
  const [lead, setLead] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (input: { name: string; leadId: number }) =>
      postJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheet)}/crews?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setName('');
      setLead('');
      setError('');
      queryClient.setQueryData(crewsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheet) });
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
    },
    onError: () => setError('That crew was refused: it needs a name no other crew or pool has and a lead on the sheet.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Type the crew name.');
      return;
    }
    if (crews.some((c) => c.name === trimmed)) {
      setError(`There is already a crew named ${trimmed}.`);
      return;
    }
    if (pools.some((p) => p.name === trimmed)) {
      setError(`There is already a pool named ${trimmed}.`);
      return;
    }
    if (!lead) {
      setError('Choose the crew lead.');
      return;
    }
    once(() => add.mutateAsync({ name, leadId: Number(lead) }));
  };

  return (
    <section data-testid="crews">
      <ul data-testid="crew-list">
        {crews.map((c) => (
          <CrewRow key={c.id} crew={c} due={crewDue(c, dueOf)} />
        ))}
      </ul>
      <form onSubmit={submit} data-testid="crew-form">
        <input
          data-testid="crew-form-name"
          placeholder="Crew name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError('');
          }}
        />
        <select
          data-testid="crew-form-lead"
          value={lead}
          onChange={(e) => {
            setLead(e.target.value);
            setError('');
          }}
        >
          <option value="">Choose a lead</option>
          {people.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
        <button type="submit" data-testid="crew-form-submit" disabled={add.isPending}>
          Add crew
        </button>
        {error && <span data-testid="crew-form-error">{error}</span>}
      </form>
    </section>
  );
}

// A crew's row: its lead, its members and its due-total, and the form that adds a member — the lead, or a person
// already in the crew, is refused.
function CrewRow({ crew, due }: { crew: Crew; due: string }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const [member, setMember] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (input: { personId: number }) =>
      postJson<Crew[]>(
        `/api/sheets/${encodeURIComponent(sheet)}/crews/${crew.id}/members?user=${encodeURIComponent(user)}`,
        input,
      ),
    onSuccess: async (saved) => {
      setMember('');
      setError('');
      queryClient.setQueryData(crewsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheet) });
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
    },
    onError: () => setError('That member was refused: a person on the sheet, not the lead and not yet in the crew.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!member) {
      setError('Choose a person.');
      return;
    }
    const id = Number(member);
    if (id === crew.lead?.id) {
      setError('That person leads the crew.');
      return;
    }
    if (crew.members.some((m) => m.id === id)) {
      setError('That person is already in the crew.');
      return;
    }
    once(() => add.mutateAsync({ personId: id }));
  };

  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <span data-testid="crew-name">{crew.name}</span> lead <span data-testid="crew-lead">{crew.lead?.name ?? ''}</span>{' '}
      due-total <span data-testid="crew-due-total">{due}</span>
      <ul>
        {crew.members.map((m) => (
          <li key={m.id} data-testid="crew-member-line">
            <span data-testid="crew-member-name">{m.name}</span>
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <select
          data-testid="crew-member-select"
          value={member}
          onChange={(e) => {
            setMember(e.target.value);
            setError('');
          }}
        >
          <option value="">Choose a person</option>
          {people.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
        <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
          Add member
        </button>
        {error && <span data-testid="crew-error">{error}</span>}
      </form>
    </li>
  );
}
