import { useCallback, useEffect, useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { STATUSES, useClaims } from '../claims/useClaims';
import { useSheetTotals } from '../claims/useSheetTotals';
import { GRADES, usePeople } from '../people/usePeople';
import { hundredths, totalDue, trimmedTotal, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheet } from '../sheet/useSheet';
import { useGradeDues } from '../summary/useGradeDues';
import { useDescendants } from './useDescendants';

type Figures = {
  /** Each person's names (their name and aliases, as names are compared). */
  names: string[][];
  /** Each person's due-total and overdue-total in cents (null: n/a), in the people's order. */
  dues: (string | null)[];
  overdues: (string | null)[];
  /** Per grade, the due-totals of the people not retired on it, added up. */
  grades: Record<string, string>;
  /** Per status, how many of the sheet's claims have it. */
  statuses: Record<string, number>;
};

const same = (a: Figures | null, b: Figures | null) =>
  a === b || (a !== null && b !== null && JSON.stringify(a) === JSON.stringify(b));

/** One sheet's figures as that sheet shows them (its header's people count, due-total and
 *  overdue-total), reported once everything they are worked out from has loaded. Renders nothing. */
function SheetFigures({ sheet, report }: { sheet: string; report: (sheet: string, figures: Figures | null) => void }) {
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const settings = useSettings(sheet);
  const today = useSheet(sheet);
  const { perPerson } = useSheetTotals(sheet);
  const byGrade = useGradeDues(sheet);
  const ready = people.isSuccess && claims.isSuccess && rates.isSuccess && settings.isSuccess && today.isSuccess;
  const key = (n: string) => n.trim().toLowerCase();
  const names = JSON.stringify((people.data ?? []).map((p) => [p.name, ...p.aliases].map(key)));
  const cents = (c: bigint | null) => (c === null ? null : c.toString());
  const dues = JSON.stringify(perPerson.map((p) => cents(p.due)));
  const overdues = JSON.stringify(perPerson.map((p) => cents(p.overdue)));
  const grades = JSON.stringify(Object.fromEntries(GRADES.map((g) => [g, totalDue(byGrade[g].map((d) => d.due))])));
  const all = claims.data ?? [];
  const statuses = JSON.stringify(Object.fromEntries(STATUSES.map((s) => [s, all.filter((c) => c.status === s).length])));
  useEffect(() => {
    report(
      sheet,
      ready
        ? { names: JSON.parse(names), dues: JSON.parse(dues), overdues: JSON.parse(overdues), grades: JSON.parse(grades), statuses: JSON.parse(statuses) }
        : null,
    );
  }, [sheet, ready, names, dues, overdues, grades, statuses, report]);
  useEffect(() => () => report(sheet, null), [sheet, report]);
  return null;
}

/** How many people there are over the tree, counting once those whose names or aliases match (as
 *  names are compared), directly or through another person. */
function distinctPeople(names: string[][]): number {
  const parent = names.map((_, i) => i);
  const find = (i: number): number => (parent[i] === i ? i : (parent[i] = find(parent[i])));
  const seen = new Map<string, number>();
  names.forEach((keys, i) =>
    keys.forEach((k) => {
      const j = seen.get(k);
      if (j === undefined) seen.set(k, i);
      else parent[find(i)] = find(j);
    }),
  );
  return names.filter((_, i) => find(i) === i).length;
}

const toCents = (c: string | null) => (c === null ? null : BigInt(c));

/**
 * The consolidated figures over the sheet and all its descendants (see useDescendants): how many
 * people (counting once those whose names or aliases match, as names are compared), and the due-total
 * and overdue-total over every person of the tree, leaving out the single highest and lowest person's
 * figure over the whole tree when three or more have one; per grade,
 * each sheet's due-totals of the people not retired on it (as its summary shows them), added up; and
 * per status, how many claims have it over the tree. While the
 * screen shows the sheet as of a date, every sheet of the tree is read as it stood at the end of that
 * date, with that date as its today. A sheet
 * with no children shows its own. Blank until every sheet's figures have loaded.
 */
function Consolidated({ sheet }: { user: string; sheet: string }) {
  const { data: descendants } = useDescendants(sheet);
  const sheets = descendants === undefined ? [] : [sheet, ...descendants];
  const [figures, setFigures] = useState<Record<string, Figures | null>>({});
  const report = useCallback((id: string, f: Figures | null) => {
    setFigures((was) => (same(was[id] ?? null, f) ? was : { ...was, [id]: f }));
  }, []);
  const all = sheets.map((id) => figures[id] ?? null);
  const ready = sheets.length > 0 && all.every((f) => f !== null);
  const loaded = all.filter((f): f is Figures => f !== null);
  return (
    <section data-testid="consolidated">
      {sheets.map((id) => (
        <SheetFigures key={id} sheet={id} report={report} />
      ))}
      <p>
        Consolidated: people <strong data-testid="consolidated-people-count">{ready ? distinctPeople(loaded.flatMap((f) => f.names)) : ''}</strong>
        , due <strong data-testid="consolidated-due-total">{ready ? trimmedTotal(loaded.flatMap((f) => f.dues.map(toCents))) : ''}</strong>
        , overdue <strong data-testid="consolidated-overdue-total">{ready ? trimmedTotal(loaded.flatMap((f) => f.overdues.map(toCents))) : ''}</strong>
      </p>
      <p>
        Due by grade:
        {GRADES.map((g) => (
          <span key={g}>
            {' '}
            {g} <strong data-testid={`consolidated-grade-${g}-due-total`}>{ready ? totalDue(loaded.map((f) => hundredths(f.grades[g]))) : ''}</strong>
          </span>
        ))}
      </p>
      <p>
        Claims by status:
        {STATUSES.map((s) => (
          <span key={s}>
            {' '}
            {s} <strong data-testid={`consolidated-status-${s}-count`}>{ready ? loaded.reduce((n, f) => n + f.statuses[s], 0) : ''}</strong>
          </span>
        ))}
      </p>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 21, Component: Consolidated });
