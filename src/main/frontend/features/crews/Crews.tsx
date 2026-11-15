import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey } from '../people/api';
import type { Person } from '../people/api';
import { listRates, personDue, rateByGrade, ratesKey } from '../rates/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { poolsKey } from '../pools/api';
import { useSheetToday } from '../sheet/api';
import { addCrew, addCrewMember, crewDueTotal, crewsKey, listCrews } from './api';
import type { Crew } from './api';
import { Fig } from '../session/Fig';

/** Everyone's due-total as their row shows it (null: n/a), by id. */
export function useDueOf(sheetId: string, user: string): Map<number, string | null> {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const cap = useCap(sheetId, today);
  const charges = useLate(sheetId, today);
  const byGrade = rateByGrade(rates.data, today);
  return new Map((people.data ?? []).map((q) => [q.id, personDue(
    liveClaims((claims.data ?? []).filter((c) => c.personId === q.id), [q]),
    rates.data, (d) => gradeOn(q, d), byGrade.get(q.grade), floor, charges, cap)]));
}

/** The sheet's crews and a form to add one: a name (trimmed; a blank one, or one used by a crew or a pool, refused) and a lead
 *  picked from the people. Each crew shows its lead, its members, and its due-total (see `crewDueTotal`),
 *  and adds a member from its row (the lead, or someone already in the crew, refused). */
export function Crews({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const crews = useQuery({ queryKey: crewsKey(sheetId), queryFn: () => listCrews(sheetId) });
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const dueOf = useDueOf(sheetId, user);
  const [name, setName] = useState('');
  const [lead, setLead] = useState('');

  const save = useMutation({
    mutationFn: (input: { name: string; lead: string }) =>
      addCrew(sheetId, user, input.name, input.lead === '' ? null : Number(input.lead)),
    onSuccess: (data) => {
      setName('');
      setLead('');
      queryClient.setQueryData(crewsKey(sheetId), data);
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();

  return (
    <section>
      <h3>Crews</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = { name, lead };
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(input);
          });
        }}
      >
        <input data-testid="crew-form-name" placeholder="Crew name" value={name}
          onChange={(e) => {
            setName(e.target.value);
            save.reset();
          }} />{' '}
        <select data-testid="crew-form-lead" value={lead}
          onChange={(e) => {
            setLead(e.target.value);
            save.reset();
          }}>
          <option value="">Lead…</option>
          {(people.data ?? []).map((person) => (
            <option key={person.id} value={String(person.id)}>{person.name}</option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-form-submit" disabled={save.isPending}>
          Add crew
        </button>
        {save.isError && (
          <span data-testid="crew-form-error">A crew needs a name not already used on the sheet by a crew or a pool, and a lead.</span>
        )}
      </form>
      <ul>
        {(crews.data ?? []).map((c) => (
          <CrewRow key={c.id} sheetId={sheetId} user={user} crew={c} people={people.data ?? []}
            dueTotal={crewDueTotal(c, dueOf)} />
        ))}
      </ul>
    </section>
  );
}

function CrewRow({ sheetId, user, crew, people, dueTotal }: { sheetId: string; user: string; crew: Crew; people: Person[]; dueTotal: string }) {
  const queryClient = useQueryClient();
  const [member, setMember] = useState('');

  const add = useMutation({
    mutationFn: (input: string) => addCrewMember(sheetId, user, crew.id, input === '' ? null : Number(input)),
    onSuccess: (data) => {
      setMember('');
      queryClient.setQueryData(crewsKey(sheetId), data);
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();

  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <strong>{crew.name}</strong>{' '}
      lead <span data-testid="crew-lead">{crew.lead}</span>{' '}
      due-total <span data-testid="crew-due-total"><Fig>{dueTotal}</Fig></span>
      <ul>
        {crew.members.map((m) => (
          <li key={m.personId} data-testid="crew-member-line">{m.name}</li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = member;
          submitOnce(() => {
            add.reset();
            return add.mutateAsync(input);
          });
        }}
      >
        <select data-testid="crew-member-select" value={member}
          onChange={(e) => {
            setMember(e.target.value);
            add.reset();
          }}>
          <option value="">Member…</option>
          {people.map((person) => (
            <option key={person.id} value={String(person.id)}>{person.name}</option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
          Add member
        </button>
        {add.isError && (
          <span data-testid="crew-error">The member could not be added: a person who neither leads the crew nor is in it already.</span>
        )}
      </form>
    </li>
  );
}
