import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { usePeopleDue } from '../header/totals';
import { shownName, usePeople } from '../people/people';
import { poolsKey } from '../pools/pools';
import { crewDueTotal, crewsKey, useCrews, type Crew, type CrewMember } from './crews';

/** One crew: its lead, its due-total, each member's line, and the picker that adds a person. The lead, or a person
 *  already in the crew, is refused with the crew's error. */
function CrewRow({ sheetId, user, crew, due }: { sheetId: string; user: string; crew: Crew; due: Map<number, string> }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState('');
  const leadPerson = crew.leadId === null ? undefined : people.data?.find((p) => p.id === crew.leadId);

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (personId: number) =>
      postJson<CrewMember>(`/api/sheets/${encodeURIComponent(sheetId)}/crews/members`, { crew: crew.name, personId, by: user }),
    onSuccess: async () => {
      setChosen('');
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <strong>{crew.name}</strong> — lead <span data-testid="crew-lead">{leadPerson === undefined ? crew.lead : shownName(leadPerson)}</span>, due total{' '}
      <span data-testid="crew-due-total">{crewDueTotal(crew, due)}</span>
      <ul>
        {crew.members.map((m) => (
          <li key={m.personId} data-testid="crew-member-line">
            {m.name}
          </li>
        ))}
      </ul>
      <select
        data-testid="crew-member-select"
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
      </select>{' '}
      <button
        type="button"
        data-testid="crew-member-add"
        disabled={add.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          add.reset();
          // nothing chosen: no person 0, so the server refuses it
          add.mutate(Number(chosen));
        }}
      >
        Add member
      </button>
      {add.isError && <span data-testid="crew-error"> Could not add this member.</span>}
    </li>
  );
}

/** The sheet's crews: the form that adds one (its name trimmed, not blank, not already taken, and a lead from the
 *  people), and each crew's row. */
function Crews({ sheetId, user }: { sheetId: string; user: string }) {
  const crews = useCrews(sheetId);
  const people = usePeople(sheetId);
  const due = usePeopleDue(sheetId);
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [lead, setLead] = useState('');

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (c: { name: string; leadId: number | null }) =>
      postJson<Crew>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`, { ...c, by: user }),
    onSuccess: async () => {
      setName('');
      setLead('');
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <section data-testid="crews">
      <h3>Crews</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          add.reset();
          add.mutate({ name, leadId: lead === '' ? null : Number(lead) });
        }}
      >
        <input
          data-testid="crew-form-name"
          placeholder="Crew name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />{' '}
        <select
          data-testid="crew-form-lead"
          value={lead}
          onChange={(e) => {
            setLead(e.target.value);
            add.reset();
          }}
        >
          <option value="">Lead…</option>
          {(people.data ?? []).map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-form-submit" disabled={add.isPending}>
          Add crew
        </button>
        {add.isError && <span data-testid="crew-form-error"> A crew's name must not be blank nor already taken, and it needs a lead.</span>}
      </form>
      {crews.isError && <p data-testid="crews-load-error">Could not load crews.</p>}
      <ul>
        {(crews.data ?? []).map((c) => (
          <CrewRow key={c.name} sheetId={sheetId} user={user} crew={c} due={due} />
        ))}
      </ul>
    </section>
  );
}

export const contributions = [SheetPage.fill({ order: 9.75, Component: Crews })];
