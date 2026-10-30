import type { Locale } from './useLocale';

// How a figure is shown under a locale. A figure is any text that is wholly a number with two
// decimals (an amount, a due figure, a total, a rate, a share, a cut). The app renders figures as
// 1234.56 everywhere (they are also what exports and audit lines carry), so the locale is applied
// once, at display: under 'de' every such text on screen is shown as 1.234,56. Entries are typed
// with a dot regardless: inputs hold values, not text, and are never touched.

const FIGURE = /^(-?)(\d+)\.(\d{2})$/;

/** Text whose element is never a displayed figure: an <option>'s text is its value, a textarea's its content. */
const SKIP = new Set(['OPTION', 'TEXTAREA', 'SCRIPT', 'STYLE']);

export function showFigure(raw: string, locale: Locale): string {
  const m = locale === 'de' ? FIGURE.exec(raw) : null;
  if (!m) return raw;
  return `${m[1]}${m[2].replace(/\B(?=(\d{3})+$)/g, '.')},${m[3]}`;
}

/** Shows every figure under `root` in the locale, now and as the screen changes, until the returned stop is called
 *  (which puts every figure back as rendered). React writes a text node's value only when its own value changes,
 *  so a node shown in the locale stays so until React writes a new value, which is then shown in turn. */
export function showFiguresIn(root: Node, locale: Locale): () => void {
  // What each changed node was rendered as, and what it is shown as.
  const shown = new Map<Text, { raw: string; text: string }>();

  const apply = (node: Text) => {
    const was = shown.get(node);
    if (was && node.nodeValue === was.text) return; // our own write
    const raw = node.nodeValue ?? '';
    const parent = node.parentElement;
    // Only an element whose whole text is the figure: not a figure inside a sentence.
    const whole = parent !== null && !SKIP.has(parent.tagName) && parent.textContent === raw;
    const text = whole ? showFigure(raw, locale) : raw;
    if (text === raw) {
      shown.delete(node);
      return;
    }
    shown.set(node, { raw, text });
    node.nodeValue = text;
  };
  const applyAll = (under: Node) => {
    if (under.nodeType === Node.TEXT_NODE) {
      apply(under as Text);
      return;
    }
    const walker = document.createTreeWalker(under, NodeFilter.SHOW_TEXT);
    for (let n = walker.nextNode(); n; n = walker.nextNode()) apply(n as Text);
  };

  if (locale === 'en') return () => {};
  applyAll(root);
  const observer = new MutationObserver((records) => {
    for (const r of records) {
      if (r.type === 'characterData') apply(r.target as Text);
      else r.addedNodes.forEach(applyAll);
    }
    for (const node of shown.keys()) if (!node.isConnected) shown.delete(node);
  });
  observer.observe(root, { subtree: true, childList: true, characterData: true });
  return () => {
    observer.disconnect();
    for (const [node, { raw, text }] of shown) if (node.nodeValue === text) node.nodeValue = raw;
    shown.clear();
  };
}
