import { useEffect, useRef, useState } from 'react';
import type { RefObject } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postOwnJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { LOCALES, localeKey, showFigure, useLocale } from './locale';
import type { Locale } from './locale';

// What a text node held before it was shown in the locale, and what it shows now; so a node React
// has since rewritten is told from one still showing our text, and a switch back can restore it.
const shownAs = new WeakMap<Text, { original: string; shown: string }>();

function localize(node: Text, locale: Locale) {
  const parent = node.parentElement;
  if (!parent || parent.closest('textarea, script, style')) return;
  const seen = shownAs.get(node);
  const current = node.nodeValue ?? '';
  const original = seen && seen.shown === current ? seen.original : current;
  const shown = showFigure(original, locale);
  if (shown !== current) node.nodeValue = shown;
  if (shown === original) shownAs.delete(node);
  else shownAs.set(node, { original, shown });
}

function localizeAll(root: Node, locale: Locale) {
  if (root.nodeType === Node.TEXT_NODE) {
    localize(root as Text, locale);
    return;
  }
  const walk = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  for (let n = walk.nextNode(); n; n = walk.nextNode()) localize(n as Text, locale);
}

/**
 * Shows every figure in the sheet body in `locale`: any text whose whole is a number with two
 * decimals, wherever a feature renders it. Features keep rendering figures as the server sends them
 * (as typed, a dot before the decimals); this rewrites them on screen only, as they appear.
 */
function useFiguresIn(anchor: RefObject<HTMLElement | null>, locale: Locale) {
  useEffect(() => {
    const root = anchor.current?.closest('[data-testid="sheet-body"]');
    if (!root) return;
    localizeAll(root, locale);
    const observer = new MutationObserver((records) => {
      for (const r of records) {
        if (r.type === 'characterData') localize(r.target as Text, locale);
        else r.addedNodes.forEach((n) => localizeAll(n, locale));
      }
    });
    observer.observe(root, { subtree: true, childList: true, characterData: true });
    return () => observer.disconnect();
  }, [anchor, locale]);
}

// The signed-in user's locale: a choice, the button that saves it, and the locale in force. It is
// the user's own, kept across sheets; it changes nothing on the sheet and writes no audit line.
function UserLocale({ user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const locale = useLocale(user);
  const current: Locale = locale.data ?? 'en';
  const [choice, setChoice] = useState<Locale | ''>('');
  const [error, setError] = useState('');
  const anchor = useRef<HTMLFormElement>(null);
  useFiguresIn(anchor, current);

  const once = useOnce();
  const save = useMutation({
    mutationFn: (chosen: Locale) => postOwnJson<{ locale: Locale }>('/api/locale', { locale: chosen }, user),
    onSuccess: (saved) => queryClient.setQueryData(localeKey(user), saved.locale),
    onError: () => setError('That locale could not be saved.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const chosen = choice || current;
    once((done) => save.mutate(chosen, { onSettled: done }));
  };

  return (
    <form ref={anchor} onSubmit={submit} data-testid="user-locale-form">
      <label>
        Locale{' '}
        <select
          data-testid="user-locale"
          value={choice || current}
          onChange={(e) => {
            setChoice(e.target.value as Locale);
            setError('');
          }}
        >
          {LOCALES.map((l) => (
            <option key={l} value={l}>
              {l}
            </option>
          ))}
        </select>
      </label>{' '}
      <button type="submit" data-testid="user-locale-save" disabled={save.isPending}>
        Save locale
      </button>{' '}
      Locale: <strong data-testid="user-locale-current">{locale.data ?? ''}</strong>
      {(error || locale.isError) && (
        <p role="alert" data-testid="user-locale-error">
          {error || 'The locale could not be read.'}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: -95, Component: UserLocale });
