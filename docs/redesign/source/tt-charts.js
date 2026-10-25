// Generatory ścieżek SVG — zwracają łańcuchy `d`
const f = (n) => Math.round(n * 100) / 100;

export function niceTicks(max, count = 3) {
  if (max <= 0) return [0];
  const raw = max / count;
  const mag = Math.pow(10, Math.floor(Math.log10(raw)));
  const step = [1, 2, 2.5, 5, 10].map(s => s * mag).find(s => s >= raw);
  const out = [];
  for (let v = 0; v <= max + step * 0.01; v += step) out.push(f(v));
  return out;
}

// punkty → path linii. ys: liczby; domain [min,max]; box {x,y,w,h}
export function linePath(ys, box, domain) {
  const [lo, hi] = domain, n = ys.length;
  return ys.map((v, i) => {
    const x = box.x + (n === 1 ? 0 : (i / (n - 1)) * box.w);
    const y = box.y + box.h - ((v - lo) / (hi - lo || 1)) * box.h;
    return (i ? 'L' : 'M') + f(x) + ' ' + f(y);
  }).join('');
}
export function areaPath(ys, box, domain) {
  const l = linePath(ys, box, domain);
  return l + 'L' + f(box.x + box.w) + ' ' + f(box.y + box.h) + 'L' + f(box.x) + ' ' + f(box.y + box.h) + 'Z';
}
export function pointAt(ys, i, box, domain) {
  const [lo, hi] = domain, n = ys.length;
  return { x: box.x + (n === 1 ? 0 : (i / (n - 1)) * box.w), y: box.y + box.h - ((ys[i] - lo) / (hi - lo || 1)) * box.h };
}

// słupek z zaokrąglonym wierzchem (rt) i spodem (rb); yb = dolna krawędź
export function barPath(x, yb, w, h, rt = 0, rb = 0) {
  if (h <= 0.01) return '';
  rt = Math.min(rt, w / 2, h); rb = Math.min(rb, w / 2, h - rt);
  const y = yb - h;
  return `M${f(x)} ${f(y + rt)}` +
    (rt ? `Q${f(x)} ${f(y)} ${f(x + rt)} ${f(y)}` : `L${f(x)} ${f(y)}`) +
    `L${f(x + w - rt)} ${f(y)}` +
    (rt ? `Q${f(x + w)} ${f(y)} ${f(x + w)} ${f(y + rt)}` : `L${f(x + w)} ${f(y)}`) +
    `L${f(x + w)} ${f(yb - rb)}` +
    (rb ? `Q${f(x + w)} ${f(yb)} ${f(x + w - rb)} ${f(yb)}` : `L${f(x + w)} ${f(yb)}`) +
    `L${f(x + rb)} ${f(yb)}` +
    (rb ? `Q${f(x)} ${f(yb)} ${f(x)} ${f(yb - rb)}` : `L${f(x)} ${f(yb)}`) + 'Z';
}

// słupki warstwowe. series: [[v per slot] per layer], zwraca [{layer, d}] ; box, max
export function stackedBars(series, box, max, opts = {}) {
  const { widthFrac = 0.56, gap = 1, rTop = 3, roundAll = false } = opts;
  const n = series[0].length, slot = box.w / n, bw = slot * widthFrac, out = [];
  for (let i = 0; i < n; i++) {
    let acc = 0;
    const layers = series.map((s, li) => ({ li, v: s[i] })).filter(l => l.v > 0);
    const total = layers.reduce((a, l) => a + l.v, 0);
    layers.forEach((l, k) => {
      const h = (l.v / max) * box.h;
      const yb = box.y + box.h - (acc / max) * box.h - (k ? gap : 0);
      const top = k === layers.length - 1, bottom = k === 0;
      const rt = roundAll ? (top ? bw / 2 : 0) : (top ? rTop : 0);
      const rb = roundAll && bottom ? bw / 2 : 0;
      out.push({ layer: l.li, slot: i, d: barPath(box.x + i * slot + (slot - bw) / 2, yb, bw, Math.max(h - (k ? gap : 0), 1), rt, rb), cx: box.x + i * slot + slot / 2, topY: box.y + box.h - (total / max) * box.h });
      acc += l.v;
    });
  }
  return out;
}

// pierścień: values → [{i, d}] (łuki do stroke), kąty od 12:00 zgodnie z ruchem wskazówek
export function ringArcs(values, cx, cy, r, gapDeg = 2) {
  const tot = values.reduce((a, b) => a + b, 0);
  let a = 0;
  const pt = (deg) => [cx + r * Math.sin(deg * Math.PI / 180), cy - r * Math.cos(deg * Math.PI / 180)];
  return values.map((v, i) => {
    const span = (v / tot) * 360;
    const a0 = a + gapDeg / 2, a1 = a + span - gapDeg / 2;
    a += span;
    const [x0, y0] = pt(a0), [x1, y1] = pt(a1);
    return { i, d: `M${f(x0)} ${f(y0)}A${r} ${r} 0 ${a1 - a0 > 180 ? 1 : 0} 1 ${f(x1)} ${f(y1)}` };
  });
}
export function ringProgress(frac, cx, cy, r) {
  frac = Math.min(Math.max(frac, 0.001), 0.9999);
  const a = frac * 360, rad = a * Math.PI / 180;
  const x = cx + r * Math.sin(rad), y = cy - r * Math.cos(rad);
  return `M${cx} ${cy - r}A${r} ${r} 0 ${a > 180 ? 1 : 0} 1 ${f(x)} ${f(y)}`;
}
export const circle = (cx, cy, r) => `M${cx - r} ${cy}a${r} ${r} 0 1 0 ${2 * r} 0a${r} ${r} 0 1 0 ${-2 * r} 0`;

// pętla trasy (makieta mapy)
export function routeLoop(box) {
  const pts = Array.from({ length: 72 }, (_, i) => {
    const t = i / 71 * Math.PI * 2;
    return [box.x + box.w / 2 + Math.sin(t) * box.w * 0.34 * (1 + 0.08 * Math.sin(3 * t)), box.y + box.h / 2 - Math.cos(t) * box.h * 0.36 * (1 + 0.06 * Math.cos(2 * t))];
  });
  return pts.map((p, i) => (i ? 'L' : 'M') + f(p[0]) + ' ' + f(p[1])).join('') + 'Z';
}
