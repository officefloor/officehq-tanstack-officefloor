import { useEffect } from 'react';
import { FIGURE, showFigure, useLocale } from './useLocale';

/** Where a text is a value being entered or picked, not a figure being shown: left as it is. */
const NOT_SHOWN = new Set(['TEXTAREA', 'OPTION', 'SCRIPT', 'STYLE']);

/** The figure each text was shown as under en, and as it was written under the locale. */
const written = new WeakMap<Text, { en: string; shown: string; locale: string }>();

/** A text that is the whole text of its element and is a figure. */
function isFigureCell(node: Text): boolean {
  const parent = node.parentElement;
  return parent !== null && !NOT_SHOWN.has(parent.tagName) && !parent.isContentEditable
    && FIGURE.test(node.data) && parent.textContent === node.data;
}

function rewrite(node: Text, locale: string) {
  const was = written.get(node);
  if (was !== undefined && node.data === was.shown) {
    // Already shown under this locale (as when this very rewrite is observed): nothing to do.
    if (was.locale === locale && node.parentElement?.textContent === node.data) return;
    // Shown under a locale before: put back as en, then (below) written again under this one.
    node.data = was.en;
  }
  written.delete(node);
  if (locale === 'en' || !isFigureCell(node)) return;
  const shown = showFigure(node.data, locale);
  if (shown === node.data) return;
  written.set(node, { en: node.data, shown, locale });
  node.data = shown;
}

function rewriteAll(root: Node, locale: string) {
  if (root.nodeType === Node.TEXT_NODE) {
    rewrite(root as Text, locale);
    return;
  }
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  for (let n = walker.nextNode(); n !== null; n = walker.nextNode()) rewrite(n as Text, locale);
}

/**
 * Shows every figure on the screen in the signed-in user's locale. A figure is the whole text of an
 * element that is a number with two decimals (an amount, a due, a total, a rate, a share, a cut);
 * every feature renders it with a dot as it always has, and under de it is shown as 1.234,56 here,
 * as React writes it, so no feature needs to know the locale. Under en the screen is as before.
 * Values in fields, and exports, are untouched: entries are typed with a dot.
 */
export function LocaleFigures({ user }: { user: string }) {
  const locale = useLocale(user);

  useEffect(() => {
    const root = document.getElementById('root') ?? document.body;
    rewriteAll(root, locale);
    if (locale === 'en') return;
    const observer = new MutationObserver((records) => {
      for (const r of records) {
        if (r.type === 'characterData') {
          rewrite(r.target as Text, locale);
        } else {
          r.addedNodes.forEach((n) => rewriteAll(n, locale));
          // A text added next to or taken from a figure changes whether it is its element's whole text.
          r.target.childNodes.forEach((n) => {
            if (n.nodeType === Node.TEXT_NODE) rewrite(n as Text, locale);
          });
        }
      }
    });
    observer.observe(root, { subtree: true, childList: true, characterData: true });
    return () => {
      observer.disconnect();
      rewriteAll(root, 'en');
    };
  }, [locale]);

  return null;
}
