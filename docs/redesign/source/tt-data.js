// Dane demo i formatery — trening-tracker (z CONTENT.md)
const NB = '\u00a0';
export const MONTHS = ['sty','lut','mar','kwi','maj','cze','lip','sie','wrz','paź','lis','gru'];
export const MONTHS_FULL = ['styczeń','luty','marzec','kwiecień','maj','czerwiec','lipiec','sierpień','wrzesień','październik','listopad','grudzień'];
export const DOW = ['nd','pn','wt','śr','czw','pt','sob'];
export const DISC = { pool: 'Basen', road: 'Rower', trainer: 'Trenażer' };

export function fmtNum(n, d = 0) {
  const s = Math.abs(n).toFixed(d);
  let [i, f] = s.split('.');
  i = i.replace(/\B(?=(\d{3})+(?!\d))/g, NB);
  return (n < 0 ? '-' : '') + i + (f ? ',' + f : '');
}
export const fmtKm = (x) => fmtNum(x, 1) + NB + 'km';
export const fmtKcal = (x) => fmtNum(Math.round(x)) + NB + 'kcal';
export const fmtBpm = (x) => Math.round(x) + NB + 'bpm';
export const fmtKmh = (x) => fmtNum(x, 1) + NB + 'km/h';
export const fmtM = (x) => fmtNum(Math.round(x)) + NB + 'm';
export const fmtPct = (x) => (x > 0 ? '+' : x < 0 ? '-' : '') + Math.abs(Math.round(x)) + '%';
export function fmtDur(sec) { // g:mm:ss
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = Math.round(sec % 60);
  return h + ':' + String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
}
export function fmtSum(min) { // 19 h 20 min / 51 min
  min = Math.round(min);
  const h = Math.floor(min / 60), m = min % 60;
  if (h === 0) return m + NB + 'min';
  return h + NB + 'h' + NB + m + NB + 'min';
}
export function fmtPace(sec) { // sek./100 m
  return Math.floor(sec / 60) + ':' + String(Math.round(sec % 60)).padStart(2, '0') + NB + '/100' + NB + 'm';
}
const D = (s) => { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d); };
export const fmtDate = (s) => { const d = D(s); return d.getDate() + NB + MONTHS[d.getMonth()] + NB + d.getFullYear(); };
export const fmtDay = (s) => { const d = D(s); return d.getDate() + NB + MONTHS[d.getMonth()]; };
export const dowOf = (s) => DOW[D(s).getDay()];
export const fmtDateTime = (s, t) => fmtDate(s) + ', ' + t;

// treningi: sec, km, kcal, pace (s/100 m), kmh, hr, hrMax
const W = (date, time, d, sec, km, kcal, extra = {}) => ({ date, time, d, sec, km, kcal, ...extra });
const m = (min) => min * 60;
export const WORKOUTS = [
  W('2026-10-22', '18:54', 'pool', m(38), 2.0, 304, { pace: 112, hr: 128 }),
  W('2026-10-20', '19:07', 'trainer', m(51), null, 459, { hr: 141, hrMax: 158 }),
  W('2026-10-19', '18:56', 'pool', m(33), 1.7, 264, { pace: 113, hr: 132 }),
  W('2026-10-17', '08:30', 'trainer', m(107), null, 1070, { hr: 147 }),
  W('2026-10-13', '19:02', 'trainer', m(54), null, 594, { hr: 139 }),
  W('2026-10-12', '18:48', 'pool', m(33), 1.8, 297, { pace: 110, hr: 130 }),
  W('2026-10-10', '09:15', 'trainer', m(83), null, 664, { hr: 143 }),
  W('2026-10-08', '18:50', 'pool', m(48), 2.4, 384, { pace: 120, hr: 127 }),
  W('2026-10-06', '19:10', 'trainer', m(63), null, 693, { hr: 140 }),
  W('2026-10-05', '18:55', 'pool', m(43), 2.3, 344, { pace: 112, hr: 129 }),
  W('2026-10-01', '18:45', 'pool', m(60), 3.2, 480, { pace: 112, hr: 126 }),
  W('2026-09-29', '17:20', 'road', m(49), 23.5, 490, { kmh: 28.8, hr: 134 }),
  W('2026-09-27', '18:50', 'pool', m(45), 2.4, 360, { pace: 113, hr: 130 }),
  W('2026-09-26', '09:43', 'road', m(163), 72.8, 1793, { kmh: 26.8, hr: 138, hrMax: 153, kmhMax: 37.5, elev: 326 }),
  W('2026-09-24', '18:40', 'pool', m(56), 2.9, 616, { pace: 117, hr: 129 }),
  W('2026-09-22', '17:35', 'road', m(90), 38.2, 910, { kmh: 25.5, hr: 136 }),
  W('2026-09-20', '18:55', 'pool', m(40), 2.1, 330, { pace: 114, hr: 128 }),
  W('2026-09-19', '10:05', 'road', m(125), 52.4, 1250, { kmh: 25.2, hr: 137 }),
  W('2026-09-17', '18:45', 'pool', m(52), 2.7, 430, { pace: 116, hr: 127 }),
  W('2026-09-15', '17:30', 'road', m(75), 33.0, 820, { kmh: 26.4, hr: 135 }),
  W('2026-09-13', '09:20', 'road', m(190), 82.1, 2050, { kmh: 25.9, hr: 140 }),
  W('2026-09-10', '18:50', 'pool', m(38), 2.0, 305, { pace: 115, hr: 129 }),
  W('2026-09-08', '17:25', 'road', m(60), 26.7, 640, { kmh: 26.7, hr: 134 }),
  W('2026-09-05', '09:30', 'road', m(150), 64.0, 1560, { kmh: 25.6, hr: 139 }),
  W('2026-09-03', '18:45', 'pool', m(44), 2.3, 360, { pace: 115, hr: 128 }),
  W('2026-09-01', '17:40', 'road', m(55), 22.9, 560, { kmh: 25.0, hr: 133 }),
];
export const GROUPS = [
  { key: '2026-10', label: 'październik 2026', count: 11, sum: '10 h 13 min · 13,5 km · 5 553 kcal'.replace(/ /g, NB).replace(NB + '·' + NB, ' · ') },
  { key: '2026-09', label: 'wrzesień 2026', count: 15, sum: '19 h 20 min · 394,9 km · 11 482 kcal'.replace(/ /g, NB).replace(NB + '·' + NB, ' · ') },
];
export const TOTALS = { count: 299, label: '353 h 20 min · 5 141,4 km · 211 715 kcal', avg: '1:10:54' };

// szczegóły — rower 26 wrz
function rng(seed) { let s = seed; return () => (s = (s * 1664525 + 1013904223) % 4294967296) / 4294967296; }
export { rng };
const r1 = rng(7);
export const HR_SERIES = Array.from({ length: 164 }, (_, i) => 120 + (i / 163) * 28 + Math.sin(i / 4.2) * 9 + (r1() - 0.5) * 6 + (i > 150 ? (i - 150) * 1.1 : 0));
export const SPEED_SERIES = Array.from({ length: 164 }, (_, i) => 27 + Math.sin(i / 3) * 4 + (r1() - 0.5) * 5);
export const ELEV_SERIES = Array.from({ length: 146 }, (_, i) => 215 + Math.sin(i / 146 * Math.PI * 6 + 0.6) * 85 + (r1() - 0.5) * 4);
export const ZONES = [
  { n: 1, from: 95, min: 6 }, { n: 2, from: 114, min: 72 }, { n: 3, from: 133, min: 75 }, { n: 4, from: 152, min: 9 }, { n: 5, from: 171, min: 0 },
];

// pulpit
export const WEEK7 = [
  { lbl: 'nd', date: '18.10', v: [0, 0, 0] }, { lbl: 'pn', date: '19.10', v: [33, 0, 0] }, { lbl: 'wt', date: '20.10', v: [0, 0, 51] },
  { lbl: 'śr', date: '21.10', v: [0, 0, 0] }, { lbl: 'czw', date: '22.10', v: [37, 0, 0] }, { lbl: 'pt', date: '23.10', v: [0, 0, 0] }, { lbl: 'sob', date: '24.10', v: [0, 0, 0] },
]; // v = [basen, rower, trenażer] w minutach

// statystyki — rok (sty–paź), godziny
export const VOL_YEAR = {
  labels: MONTHS.slice(0, 10),
  pool: [5.8, 4.8, 5.6, 6.2, 5.8, 5.6, 5.6, 6.2, 5.2, 4.2],
  road: [0, 0, 0, 12.5, 16.5, 15, 13.5, 18, 14, 0],
  trainer: [10, 9, 10.2, 0, 0, 0, 0, 0, 0, 6],
};
export const SHARE = [
  { d: 'pool', pct: 31, t: '56 h 29 min' }, { d: 'road', pct: 50, t: '89 h 09 min' }, { d: 'trainer', pct: 19, t: '34 h 23 min' },
].map(s => ({ ...s, t: s.t.replace(/ /g, NB) }));
export const SHARE_TOTAL = '180 h 1 min'.replace(/ /g, NB);
export const WEEKDAYS = { labels: ['pn', 'wt', 'śr', 'czw', 'pt', 'sob', 'nd'], h: [27, 41, 0, 29, 0, 81, 0] };
export const DAYTIME = { labels: ['0–4', '4–8', '8–12', '12–16', '16–20', '20–24'], h: [0, 0, 81, 0, 97, 0] };
const r2 = rng(21);
export const CUM = (() => {
  const f = (end, days) => Array.from({ length: days }, (_, i) => { const t = i / (days - 1); return end * (t * t * (3 - 2 * t) * 0.8 + t * 0.2); });
  return { y26: f(2600, 297), y25: f(2480, 365) };
})();
const trend = (n, a, b, noise, seed) => { const r = rng(seed); return Array.from({ length: n }, (_, i) => a + (b - a) * (i / (n - 1)) + (r() - 0.5) * noise); };
export const TREND = {
  pool: trend(45, 145, 112, 12, 3), // s/100 m
  road: trend(40, 24, 27, 2.4, 5),   // km/h
  trainer: [27.5],                   // za mało danych
  form: trend(40, 17, 21, 1.6, 9),
};
export const movAvg = (a, k = 5) => a.map((_, i) => { const s = a.slice(Math.max(0, i - k + 1), i + 1); return s.reduce((x, y) => x + y, 0) / s.length; });

// heatmapa: 26 tygodni × 7 dni (pn–nd). Komórka: null | {d, lvl 1..4}
export const HEAT = (() => {
  const r = rng(11);
  const cols = [];
  for (let w = 0; w < 26; w++) {
    const col = Array(7).fill(null);
    const mon = w >= 15 && w <= 22; // lato
    if (r() > 0.1) col[1] = { d: 'pool', lvl: 1 + Math.floor(r() * 4) };
    if (r() > 0.1) col[3] = { d: 'pool', lvl: 1 + Math.floor(r() * 4) };
    if (r() > 0.08) col[5] = { d: mon ? 'road' : (w < 12 ? 'trainer' : (w > 22 ? 'trainer' : 'road')), lvl: 2 + Math.floor(r() * 3) };
    if (r() > 0.8) col[0] = { d: 'trainer', lvl: 1 + Math.floor(r() * 2) };
    cols.push(col);
  }
  cols[25] = [null, { d: 'pool', lvl: 1 }, { d: 'trainer', lvl: 2 }, null, { d: 'pool', lvl: 2 }, null, null];
  return cols;
})();

// podsumowania
export const WEEKS = [
  { range: '19 paź 2026 – 25 paź 2026', min: 122, km: 3.8, kcal: 1027, n: 3, days: 3, streak: 2, d: [{ d: 'pool', min: 71, km: 3.8, kcal: 568 }, { d: 'trainer', min: 51, km: null, kcal: 459 }], delta: { t: -37, km: 109, kcal: -48 }, best: 'Trenażer, 20 paź 2026 · 0:51:00' },
  { range: '12 paź 2026 – 18 paź 2026', min: 194, km: 1.8, kcal: 1961, n: 3, days: 3, streak: 2, d: [{ d: 'pool', min: 33, km: 1.8, kcal: 297 }, { d: 'trainer', min: 161, km: null, kcal: 1664 }], delta: { t: -18, km: -62, kcal: -6 }, best: 'Trenażer, 17 paź 2026 · 1:47:00' },
  { range: '5 paź 2026 – 11 paź 2026', min: 237, km: 4.7, kcal: 2054, n: 4, days: 4, streak: 2, d: [{ d: 'pool', min: 91, km: 4.7, kcal: 728 }, { d: 'trainer', min: 146, km: null, kcal: 1326 }], delta: { t: 12, km: 20, kcal: 8 }, best: 'Trenażer, 10 paź 2026 · 1:23:00' },
  { range: '28 wrz 2026 – 4 paź 2026', min: 208, km: 15.2, kcal: 1840, n: 4, days: 4, streak: 3, d: [{ d: 'pool', min: 60, km: 3.2, kcal: 480 }, { d: 'road', min: 148, km: 12.0, kcal: 1360 }], delta: { t: -9, km: -30, kcal: -4 }, best: 'Rower, 29 wrz 2026 · 0:49:00' },
  { range: '21 wrz 2026 – 27 wrz 2026', min: 342, km: 96.8, kcal: 3100, n: 5, days: 4, streak: 2, d: [{ d: 'pool', min: 45, km: 2.4, kcal: 360 }, { d: 'road', min: 297, km: 94.4, kcal: 2740 }], delta: { t: 21, km: 35, kcal: 18 }, best: 'Rower, 26 wrz 2026 · 2:43:00' },
  { range: '14 wrz 2026 – 20 wrz 2026', min: 280, km: 56.6, kcal: 2400, n: 4, days: 4, streak: 2, d: [{ d: 'pool', min: 40, km: 2.1, kcal: 330 }, { d: 'road', min: 240, km: 54.5, kcal: 2070 }], delta: { t: 5, km: 8, kcal: 3 }, best: 'Rower, 19 wrz 2026 · 2:05:00' },
  { range: '7 wrz 2026 – 13 wrz 2026', min: 262, km: 80.2, kcal: 2200, n: 3, days: 3, streak: 1, d: [{ d: 'road', min: 262, km: 80.2, kcal: 2200 }], delta: { t: -4, km: 12, kcal: -2 }, best: 'Rower, 13 wrz 2026 · 3:10:00' },
  { range: '31 sie 2026 – 6 wrz 2026', min: 275, km: 70.0, kcal: 2300, n: 4, days: 4, streak: 2, d: [{ d: 'road', min: 275, km: 70.0, kcal: 2300 }], delta: { t: 8, km: 10, kcal: 6 }, best: 'Rower, 5 wrz 2026 · 2:30:00' },
];
export const RECORDS = {
  pool: [['Najdłuższy dystans', '3,2 km', '1 paź 2026'], ['Najdłuższy czas', '1:00:00', '1 paź 2026'], ['Najlepsze tempo', '1:50 /100 m', '12 paź 2026', true], ['Najwięcej kalorii', '660 kcal', '9 mar 2026']],
  road: [['Najdłuższy dystans', '99,1 km', '16 maj 2026'], ['Najdłuższy czas', '3:23:00', '6 wrz 2025'], ['Najlepsza średnia prędkość', '30,4 km/h', '1 sie 2026'], ['Najwięcej kalorii', '2 316 kcal', '26 kwi 2025'], ['Największe przewyższenie', '995 m', '8 sie 2026']],
  trainer: [['Najdłuższy czas', '1:58:00', '14 lut 2026'], ['Najwięcej kalorii', '1 298 kcal', '14 lut 2026']],
  periods: [['Tydzień: dystans', '134,3 km', '27 lip 2026'], ['Tydzień: czas', '6 h 0 min', '10 sie 2026']],
};
export const GOALS = [
  { d: 'road', name: 'Rower · Dystans (km) · Miesięczny', cur: 0, goal: 300, unit: 'km', curTxt: '0,0 km', goalTxt: '300,0 km', fc: 'W tym tempie wyjdzie 93,0 km', left: 'Zostało 7 dni', hist: [1, 1, 1, 1, 1, 1], streak: 6 },
  { d: 'all', name: 'Wszystko · Liczba treningów · Tygodniowy', cur: 3, goal: 3, unit: '', curTxt: '3', goalTxt: '3', done: true, left: 'Zostało 1 dzień', hist: [1, 0, 1, 1, 1, 1], streak: 4 },
  { d: 'pool', name: 'Basen · Dystans (km) · Tygodniowy', cur: 3.8, goal: 5, unit: 'km', curTxt: '3,8 km', goalTxt: '5,0 km', fc: 'W tym tempie wyjdzie 4,4 km', left: 'Zostało 1 dzień', hist: [1, 0, 1, 0, 1, 1], streak: 2 },
  { d: 'all', name: 'Wszystko · Czas (godziny) · Miesięczny', cur: 10.2, goal: 20, unit: 'h', curTxt: '10 h 13 min', goalTxt: '20 h', fc: 'W tym tempie wyjdzie 14 h 20 min', left: 'Zostało 7 dni', hist: [1, 1, 0, 1, 1, 0], streak: 0 },
];
export const DIAG = [['ExerciseSession', 299], ['HeartRate', 41236], ['Distance', 214], ['TotalCaloriesBurned', 299], ['ElevationGained', 96], ['ExerciseRoute', 38], ['Speed', 12870]];
