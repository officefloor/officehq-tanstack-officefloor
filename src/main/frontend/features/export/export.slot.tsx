import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { gradeOn, liveHolds, usePeople } from '../people/people';
import { claimDue, heldParts, personDueTotal, rateOn, sumDue, useRates } from '../rates/rates';
import { periodOf, personPeriodCarried, personPeriodDueTotal, usePeriods } from '../periods/periods';
import { useCap, useFloor, useLate } from '../access/access';
import { useSheetToday } from '../sheet/sheet';
import { getJson } from '../../api/http';
import type { Pool } from '../pools/pools';
import { downloadCsv } from './csv';
import { useCutTotals } from '../pools/pools';
import { treePeople } from '../treesearch/treeSearch';
import type { PolicyVersion } from '../policy/policy';

// Downloads the people as a CSV file: a header line, then one line per person in list order,
// retired people included, with the grade in force today, the retired-from date ('' when not
// retired) and the due-total as the list shows it. Lines end with a line feed. While an as-of date
// is set every figure is as of it, as the list shows it then; while a preview date is set every
// figure is the preview figure, that date standing in for the sheet's today, as the list shows it.
function ExportPeople({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);

  const download = () => {
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people) {
      const own = claims.filter((c) => c.personId === p.id);
      lines.push([p.name, p.grade, p.retiredFrom, personDueTotal(rates, p.grades, today, own, liveHolds(p, today), floor, late, cap, heldParts(p.id, people, claims, rates, today, floor, late), cutOf(p.id))]);
    }
    downloadCsv(`people-${sheetId}.csv`, lines);
  };

  return (
    <p>
      <button type="button" data-testid="export-people" onClick={download}>
        Export people (CSV)
      </button>
    </p>
  );
}

// Downloads the claims as a CSV file: a header line, then one line per claim, people in list order
// (retired people included) and each person's claims oldest first, with the number, due and period
// as the claim's row shows them (preview figures while a preview date is set). Quoted and ended as
// the people export.
function ExportClaims({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const download = () => {
    const lines = [['person', 'number', 'date', 'amount', 'due', 'status', 'period']];
    for (const p of people) {
      // The sheet's claims come newest first.
      const own = claims.filter((c) => c.personId === p.id).reverse();
      for (const c of own) {
        const grade = gradeOn(p.grades, c.date);
        const due = claimDue(c, rateOn(rates, grade, c.date, c.id), floor, late, grade);
        lines.push([p.name, c.number, c.date, c.amount, due, c.status, periodOf(periods, c.dueBy)?.name ?? '']);
      }
    }
    downloadCsv(`claims-${sheetId}.csv`, lines);
  };

  return (
    <p>
      <button type="button" data-testid="export-claims" onClick={download}>
        Export claims (CSV)
      </button>
    </p>
  );
}

// Downloads the pools as a CSV file: a header line, then one line per member, pools in the order
// added and each pool's members in the order added, a member pool as pool:<name> and the cut as
// the pool's row shows it ('' when none). Quoted and ended as the people export. The pools are read
// afresh when pressed, so a change just made is always in the file; while an as-of date is set they
// are read as of it (members, pot and cuts as they stood then), and while a preview date is set they
// are read at it.
function ExportPools({ sheetId }: { sheetId: string }) {
  const download = async () => {
    const { pools } = await getJson<{ pools: Pool[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`);
    const lines = [['pool', 'member', 'share', 'cut']];
    for (const p of [...pools].sort((a, b) => a.added - b.added)) {
      for (const m of p.members) {
        lines.push([p.name, m.memberPool ? `pool:${m.memberPool}` : m.name, m.share, m.cut]);
      }
    }
    downloadCsv(`pools-${sheetId}.csv`, lines);
  };

  return (
    <p>
      <button type="button" data-testid="export-pools" onClick={() => void download()}>
        Export pools (CSV)
      </button>
    </p>
  );
}

// Downloads the period figures as a CSV file: a header line, then one line per person and period,
// people in list order (retired people included) and periods in the order added, with the figure
// and carried figure as the person's row shows them. Quoted and ended as the people export.
function ExportPeriods({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const download = () => {
    const lines = [['person', 'period', 'figure', 'carried']];
    const added = [...periods].sort((a, b) => a.added - b.added);
    for (const p of people) {
      for (const period of added) {
        lines.push([
          p.name,
          period.name,
          personPeriodDueTotal(period, p, claims, rates, floor, late),
          personPeriodCarried(period, periods, p, claims, rates, floor, late),
        ]);
      }
    }
    downloadCsv(`periods-${sheetId}.csv`, lines);
  };

  return (
    <p>
      <button type="button" data-testid="export-periods" onClick={download}>
        Export periods (CSV)
      </button>
    </p>
  );
}

// Downloads the people of the whole tree as a CSV file: a header line, then one line per person,
// this sheet's people first in list order, then the other sheets' of its tree by sheet id, with the
// grade in force today and the due-total as their sheet's list shows it. Every sheet is read afresh
// when pressed. Quoted and ended as the people export.
function ExportTree({ sheetId }: { sheetId: string }) {
  const download = async () => {
    const lines = [['sheet', 'name', 'grade', 'due-total']];
    for (const t of await treePeople(sheetId)) lines.push([t.sheetId, t.person.name, t.person.grade, t.dueTotal]);
    downloadCsv(`tree-${sheetId}.csv`, lines);
  };

  return (
    <p>
      <button type="button" data-testid="export-tree" onClick={() => void download()}>
        Export tree people (CSV)
      </button>
    </p>
  );
}

// Downloads the policy versions as a CSV file: a header line, then one line per version, oldest first,
// with its number, from-date ('' for none), the setting it changed with its value, the count of claims
// whose policy version it is (DELETED claims counting nowhere) and the due of those claims, as their
// rows show it, added up. The versions are read afresh when pressed. Quoted and ended as the people export.
function ExportPolicy({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const download = async () => {
    const { versions } = await getJson<{ versions: PolicyVersion[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/policy`);
    // A claim's policy version: the number of versions whose from-date is on or before its date.
    const versionOn = (date: string) => versions.filter((v) => v.from === '' || v.from <= date).length;
    const dues = new Map<number, string[]>();
    for (const p of people) {
      for (const c of claims) {
        if (c.personId !== p.id || c.status === 'DELETED') continue;
        const grade = gradeOn(p.grades, c.date);
        const n = versionOn(c.date);
        dues.set(n, [...(dues.get(n) ?? []), claimDue(c, rateOn(rates, grade, c.date, c.id), floor, late, grade)]);
      }
    }
    const lines = [['version', 'from', 'key', 'value', 'claims', 'due-total']];
    for (const v of versions) lines.push([String(v.version), v.from, v.key, v.value, String(v.claims), sumDue(dues.get(v.version) ?? [])]);
    downloadCsv(`policy-${sheetId}.csv`, lines);
  };

  return (
    <p>
      <button type="button" data-testid="export-policy" onClick={() => void download()}>
        Export policy versions (CSV)
      </button>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 2, Component: ExportPeople }),
  SheetBody.fill({ order: 2.1, Component: ExportClaims }),
  SheetBody.fill({ order: 2.2, Component: ExportPools }),
  SheetBody.fill({ order: 2.3, Component: ExportPeriods }),
  SheetBody.fill({ order: 2.4, Component: ExportTree }),
  SheetBody.fill({ order: 2.5, Component: ExportPolicy }),
];
