# BRIEF: trening-tracker

## Produkt w jednym zdaniu
Prywatna apka na Androida, która pokazuje treningi z zegarka Huawei Watch GT5 (basen, rower na dworze,
trenażer) w czytelnych tabelach, wykresach i podsumowaniach.

## Użytkownik
Jedna osoba: Albert, trenuje trzy dyscypliny, lubi liczby i porównania („czy jest postęp?").
Apka ma mu służyć po treningu (szybkie zerknięcie) i raz na tydzień czy miesiąc (dłuższy przegląd).
Ewentualnie kilku znajomych dostanie APK.

## Jak działają dane
```
zegarek → Huawei Health → Health Sync → Health Connect → apka → lokalna baza (Room)
```
- Nie ma serwera, kont ani logowania. Wszystko zostaje na telefonie.
- Health Connect bywa dziurawy: **nie każdy trening ma każdą daną**.
  - basen: czas, dystans, kalorie, tętno, tempo
  - trenażer: czas, kalorie, tętno (zwykle **bez dystansu i prędkości**)
  - rower na dworze: czas, dystans, kalorie, tętno, prędkość, przewyższenie, czasem trasa GPS
- Dlatego projekt musi dobrze wyglądać także wtedy, gdy połowy liczb nie ma.
  **Brakujące dane znikają, nie zamieniają się w zera ani w puste kratki.**

## Trzy dyscypliny i ich kolory
| Dyscyplina | Nazwa w UI | Kolor dzisiaj | Rola |
|---|---|---|---|
| POOL | Basen | niebieski `#1E88E5` | tempo /100 m zamiast prędkości |
| ROAD | Rower | zielony `#43A047` | jedyna z trasą na mapie i przewyższeniem |
| TRAINER | Trenażer | pomarańczowy `#FB8C00` | zwykle tylko czas, tętno, kalorie |

Kolory mają być rozpoznawalne wszędzie: kropka przy treningu, słupek na wykresie, wycinek pierścienia,
linia na wykresie trendu, obwódka kafelka. Odcienie można zmienić, podział nie.

## Co dzisiaj jest nie tak (powód redesignu)
- Wszystko to ten sam szary prostokąt: Pulpit, Statystyki, Podsumowania i Rekordy wyglądają identycznie.
- Brak hierarchii: liczba, która jest najważniejsza, wygląda tak samo jak podpis pod nią.
- Tabela treningów przewija się w poziomie i gubi kolumny na wąskim ekranie.
- Wykresy mają domyślny styl biblioteki, każdy trochę inny; osie i etykiety bywają za gęste.
- Strefy tętna to prowizoryczne paski z tekstem po bokach, łamią się na dwie linie.
- Pusty stan to jedno zdanie na środku, bez pomysłu.
- Zero charakteru: nic nie mówi „to apka o sporcie".

## Czego NIE zmieniamy
- Zestawu funkcji i ekranów (lista w `SCREENS.md`) — redesign dotyczy wyglądu i układu, nie zakresu.
- Języka: interfejs po polsku, formaty polskie.
- Zasady „brak danych = brak pola".
- Technologii: Jetpack Compose + Material 3, wykresy Vico i własny Canvas, mapa OpenStreetMap.

## Ograniczenia techniczne, o których warto pamiętać
- Dynamiczne kolory Material You (Android 12+) są dzisiaj włączone i barwią cały interfejs od tapety.
  Jeśli projekt ma mieć **własną, stałą paletę**, powiedz to wprost — to świadoma decyzja do podjęcia.
- Wykresy słupkowe i liniowe rysuje Vico: da się ustawić kolory, grubości, kształty słupków,
  formaty etykiet osi i odstępy. Nie da się łatwo zrobić dowolnych ozdobników wewnątrz wykresu.
- Heatmapa, pierścień dyscyplin i paski stref tętna są rysowane ręcznie na `Canvas`, więc tu jest
  pełna swoboda.
- Mapa to kafelki OpenStreetMap (osmdroid) — wygląd kafelków jest narzucony, można stylować tylko
  ramkę, ślad trasy i to, co jest wokół.
- Dolna nawigacja ma dzisiaj 4 pozycje: Pulpit, Treningi, Statystyki, Więcej.

## Miary sukcesu
1. Po otwarciu apki w 2 sekundy widać: ile zrobiłem w tym tygodniu i czy to więcej niż poprzednio.
2. Na liście treningów da się porównać treningi bez przewijania w poziomie.
3. Każdy wykres da się zrozumieć bez czytania opisu pod spodem.
4. Jasny i ciemny motyw wyglądają jak dwa warianty jednego projektu, a nie jak dwie różne apki.
