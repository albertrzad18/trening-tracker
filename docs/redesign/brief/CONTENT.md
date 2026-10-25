# Treści i dane do makiet

Wszystkie teksty interfejsu są po polsku. Poniżej prawdziwe napisy z apki i realne dane,
których można użyć w makietach. Liczby pochodzą z wygenerowanych danych demo (18 miesięcy treningów),
więc są wiarygodne, ale nie są czyimiś prawdziwymi treningami.

**Emoji do usunięcia.** W kilku miejscach są dzisiaj `🏆`, `🎉` i `✔`/`✘` — to stan obecny, nie wzór.
W nowym projekcie nie ma emoji; te miejsca mają dostać ikony z jednego zestawu albo samą typografię.

## Formaty (obowiązkowe)
| Co | Format | Przykład |
|---|---|---|
| dystans | spacja jako separator tysięcy, przecinek dziesiętny | `1 234,5 km`, `3,8 km` |
| czas pojedynczego treningu | `g:mm:ss` | `1:23:45`, `0:38:00` |
| suma czasu (wiele treningów) | godziny i minuty | `19 h 20 min`, `51 min` |
| data | dzień, skrót miesiąca, rok | `24 paź 2026` |
| data z godziną | | `22 paź 2026, 18:54` |
| zakres tygodnia | | `19 paź 2026 – 25 paź 2026` |
| miesiąc | | `październik 2026` |
| tempo (basen) | | `2:15 /100 m` |
| prędkość | | `26,8 km/h` |
| kalorie | | `1 793 kcal` |
| tętno | | `138 bpm` |
| zmiana | ze znakiem | `+55%`, `-37%` |
| udział | bez znaku | `31%` |
| przewyższenie | | `326 m` |

## Nazwy i etykiety
- Dyscypliny: **Basen**, **Rower**, **Trenażer**
- Nawigacja: **Pulpit**, **Treningi**, **Statystyki**, **Więcej**
- Okresy: **Tydzień**, **Miesiąc**, **Rok**, **Wszystko**
- Metryki wykresu: **Czas**, **Dystans**, **Kalorie**
- Kolumny tabeli: **Data**, **Czas**, **Dystans**, **Kalorie**, **Tempo / prędkość**, **Tętno**
- Metryki w szczegółach: **Czas**, **Dystans**, **Kalorie**, **Śr. tętno**, **Maks. tętno**,
  **Śr. prędkość**, **Maks. prędkość**, **Tempo**, **Przewyższenie**

## Pulpit — przykładowe dane
```
Ten tydzień
2:02:00
3,8 km · 1 027 kcal
-37% vs poprzedni tydzień

Basen     Treningi: 2   3,8 km
Rower     Treningi: 0
Trenażer  Treningi: 1

Ostatnie 7 dni: 18.10 –, 19.10 33 min (basen), 20.10 51 min (trenażer),
21.10 –, 22.10 37 min (basen), 23.10 –, 24.10 –

Ostatnie treningi
Basen     22 paź 2026, 18:54   0:38:00   2,0 km
Trenażer  20 paź 2026, 19:07   0:51:00
Basen     19 paź 2026, 18:56   0:33:00   1,7 km
```

## Treningi — przykładowe wiersze
```
październik 2026 · 11        10 h 13 min · 13,5 km · 5 553 kcal
22 paź 2026  ●basen     0:38:00   2,0 km   304 kcal   1:52 /100 m   128 bpm
20 paź 2026  ●trenażer  0:51:00            459 kcal                 141 bpm
19 paź 2026  ●basen     0:33:00   1,7 km   264 kcal   1:53 /100 m   132 bpm
17 paź 2026  ●trenażer  1:47:00          1 070 kcal                 147 bpm
wrzesień 2026 · 15           19 h 20 min · 394,9 km · 11 482 kcal
29 wrz 2026  ●rower     0:49:00  23,5 km   490 kcal   28,8 km/h     134 bpm
26 wrz 2026  ●rower     2:43:00  72,8 km 1 793 kcal   26,8 km/h     138 bpm
24 wrz 2026  ●basen     0:56:00   2,9 km   616 kcal   1:57 /100 m   129 bpm

Suma (299): 353 h 20 min · 5 141,4 km · 211 715 kcal
Średni czas: 1:10:54
```

## Szczegóły treningu — przykład (rower)
```
Rower
26 wrz 2026, 09:43

Czas 2:43:00      Dystans 72,8 km
Kalorie 1 793 kcal  Śr. tętno 138 bpm
Maks. tętno 153 bpm  Śr. prędkość 26,8 km/h
Maks. prędkość 37,5 km/h  Przewyższenie 326 m

Strefy tętna (tętno maksymalne 190)
Strefa 1 · 95+    6 min
Strefa 2 · 114+   1 h 12 min
Strefa 3 · 133+   1 h 15 min
Strefa 4 · 152+   9 min
Strefa 5 · 171+   0 min

Na tle Twojej średniej
Czas: +55%
Dystans: +59%
```

Przykład drugi (trenażer, dużo braków): `Trenażer, 20 paź 2026, 19:07`, czas `0:51:00`,
kalorie `459 kcal`, śr. tętno `141 bpm`, maks. tętno `158 bpm` — **bez dystansu, prędkości i trasy**.

## Statystyki — przykładowe wartości
- Objętość treningowa (rok, czas): miesiące sty–paź, wartości od `8 h` do `24 h`,
  z podziałem: basen ~5 h w każdym miesiącu, rower 0 h zimą i do 18 h latem, trenażer odwrotnie.
- Udział dyscyplin: Basen `31% · 56 h 29 min`, Rower `50% · 89 h 09 min`, Trenażer `19% · 34 h 23 min`,
  w środku pierścienia `180 h 1 min`.
- Dni tygodnia: pon `27 h`, wt `41 h`, śr `0`, czw `29 h`, pt `0`, sob `81 h`, niedz `0`.
- Pora dnia: `8–12` → `81 h`, `16–20` → `97 h`, reszta zero.
- Dystans narastająco: 2026 do `2 600 km` (dzień 297), 2025 do `2 480 km` (cały rok).
- Trend basenu: tempo spada z `2:25 /100 m` do `1:52 /100 m` w ciągu 18 miesięcy.
- Forma (rower): rośnie z `17` do `21` km/h na 100 uderzeń.

## Podsumowania — przykład karty
```
19 paź 2026 – 25 paź 2026
2 h 2 min · 3,8 km · 1 027 kcal
Treningi: 3
Dni z treningiem: 3
Najdłuższa seria: 2 dni
Na tle poprzedniego okresu: Czas: -37% · Dystans: +109% · Kalorie: -48%
● Basen     1 h 11 min · 3,8 km · 568 kcal
● Trenażer  51 min · 459 kcal
Najlepszy trening: Trenażer, 20 paź 2026 · 0:51:00
```

## Rekordy — przykład
```
● Basen
Najdłuższy dystans          3,2 km       1 paź 2026
Najdłuższy czas             1:00:00      1 paź 2026
Najlepsze tempo             1:50 /100 m  12 paź 2026
Najwięcej kalorii           660 kcal     9 mar 2026

● Rower
Najdłuższy dystans          99,1 km      16 maj 2026
Najdłuższy czas             3:23:00      6 wrz 2025
Najlepsza średnia prędkość  30,4 km/h    1 sie 2026
Najwięcej kalorii         2 316 kcal     26 kwi 2025
Największe przewyższenie    995 m        8 sie 2026

● Trenażer
Najdłuższy czas             1:58:00      14 lut 2026
Najwięcej kalorii         1 298 kcal     14 lut 2026

Najlepsze okresy
Tydzień: dystans   134,3 km   27 lip 2026
Tydzień: czas      6 h 0 min  10 sie 2026
```

## Cele — przykład
```
● Rower · Dystans (km) · Miesięczny
0,0 km z 300,0 km
Zostało 7 dni
Historia: ✔ ✔ ✔ ✔ ✔ ✔
Seria: 6 okresów pod rząd
```
Inne możliwe cele: `Wszystko · Liczba treningów · Tygodniowy` (3 treningi),
`Basen · Dystans (km) · Tygodniowy` (5 km), `Wszystko · Czas (godziny) · Miesięczny` (20 h).

## Teksty pustych stanów i komunikatów
- Pulpit pusto: **Brak treningów** / „Treningi pojawią się tutaj po synchronizacji z Health Connect.
  Upewnij się, że Health Sync przesyła treningi z Huawei Health." / przycisk **Synchronizuj teraz**
- Lista: „Brak treningów dla wybranych filtrów."
- Wykres: „Brak danych w tym okresie." / „Potrzebne są co najmniej dwa treningi z tymi danymi."
- Statystyki pusto: „Brak treningów do pokazania. Zsynchronizuj dane w zakładce Więcej."
- Rekordy pusto: „Rekordy pojawią się, gdy w bazie będą treningi."
- Cele pusto: „Nie masz jeszcze żadnego celu. Dodaj pierwszy przyciskiem poniżej."
- Podsumowanie pustego okresu: „Brak treningów w tym okresie."
- Brak uprawnień: „Apka potrzebuje uprawnień do odczytu treningów z Health Connect.
  Dane zostają na Twoim telefonie." / przycisk **Nadaj uprawnienia**
- Brak Health Connect: „Health Connect jest niedostępny lub wymaga aktualizacji. Zainstaluj go albo
  zaktualizuj w Sklepie Play." / przycisk **Zainstaluj Health Connect**
- Szczegóły, gdy treningu nie ma: „Nie znaleziono treningu."
- Synchronizacja: „Ostatnia synchronizacja: 24 paź 2026, 21:14", „Jeszcze nie synchronizowano",
  „Ostatni błąd: …", przycisk **Synchronizuj teraz**

## Opisy wykresów (zostają, ale można je skrócić)
- Objętość treningowa: „Minuty treningu, słupki ułożone według dyscyplin"
- Kalendarz aktywności: „Ostatnie 26 tygodni: kolumna to tydzień, kwadrat to dzień"
- Udział dyscyplin: „Podział łącznego czasu treningów"
- Dni tygodnia: „W które dni trenujesz najwięcej"
- Pora dnia: „O której godzinie zaczynasz treningi"
- Trend (basen): „Tempo /100 m: niżej znaczy szybciej. Gruba linia to średnia z ostatnich pięciu treningów."
- Trend (rower): „Średnia prędkość km/h. Gruba linia to średnia z ostatnich pięciu treningów."
- Forma: „Km/h na 100 uderzeń tętna. Rośnie, gdy przy tym samym tętnie jedziesz szybciej."
- Dystans narastająco: „Kilometry od 1 stycznia: 2026 na tle 2025"
- Strefy tętna: „Ile czasu trening spędził w każdej strefie. Granice liczymy z tętna maksymalnego z ustawień."
