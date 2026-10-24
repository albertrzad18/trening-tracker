# Materiały do redesignu

Komplet do wrzucenia Claude'owi. Kolejność: wklej treść `PROMPT.md`, załącz trzy pliki `.md`
i zrzuty ekranu z `docs/screenshots/`.

| Plik | Co zawiera |
|---|---|
| [`PROMPT.md`](PROMPT.md) | Gotowy prompt do skopiowania (zadanie, ograniczenia, forma odpowiedzi) |
| [`BRIEF.md`](BRIEF.md) | Kontekst produktu, użytkownik, kolory dyscyplin, co jest dziś nie tak |
| [`SCREENS.md`](SCREENS.md) | Wszystkie ekrany: zawartość, interakcje, stany |
| [`CONTENT.md`](CONTENT.md) | Polskie teksty, formaty i realne dane do makiet |

## Zrzuty ekranu (stan przed redesignem)

| Plik | Ekran |
|---|---|
| `00-brak-uprawnien.png` | Stan startowy bez uprawnień |
| `01-pulpit.png` | Pulpit |
| `02-treningi.png` | Tabela treningów |
| `03-statystyki.png` | Statystyki: objętość i kalendarz aktywności |
| `12-statystyki-filtr.png` | Statystyki z filtrem jednej dyscypliny i okresem „Wszystko" |
| `04-trasa.png` | Szczegóły: mapa trasy i profil wysokości |
| `07-szczegoly.png` | Szczegóły: metryki i wykres tętna |
| `08-strefy.png` | Szczegóły: strefy tętna i prędkość |
| `05-rekordy.png` | Rekordy |
| `06-podsumowania.png` | Podsumowania |
| `09-wiecej.png` | Więcej |
| `10-cele.png` | Cele |
| `11-ustawienia.png` | Ustawienia |
| `13-ciemny-motyw.png` | Statystyki w ciemnym motywie |

Zrzuty zrobione na danych demo (18 miesięcy wygenerowanych treningów), nie na prawdziwych.

## Po otrzymaniu projektu

1. Zapisz wynik (tokeny + opis komponentów) jako `docs/design/SYSTEM.md`, a makiety jako artefakt lub HTML w `docs/design/`.
2. Przenieś tokeny do `ui/theme/Theme.kt` — tam jest dzisiaj paleta i włączone dynamiczne kolory Material You.
3. Komponenty wspólne idą do `ui/common/`, style wykresów do `ui/charts/`.
4. Kolory dyscyplin muszą zostać w jednym miejscu (`PoolBlue`, `RoadGreen`, `TrainerOrange`), bo bierze je
   cała apka: kropki, kafelki, słupki, linie, pierścień i ślad trasy.
