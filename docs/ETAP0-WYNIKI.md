# Etap 0: wyniki rozpoznania danych

Sprawdzone ekranem debug na telefonie, z danymi z Huawei Watch GT5 przez Huawei Health → Health Sync → Health Connect.
Raport zawiera tylko informację, jakie dane są obecne. Nie ma w nim wartości ani dat treningów.

**Próba jest mała:** 1 trening basenowy i 1 na trenażerze. Wnioski do potwierdzenia na większej liczbie treningów.

## Co przechodzi

| Dana | Basen | Trenażer | Rower (dwór) |
|---|---|---|---|
| Tętno | ✅ | ✅ | nie sprawdzono |
| Dystans | ✅ | ❌ | nie sprawdzono |
| Kalorie łączne | ✅ | ✅ | nie sprawdzono |
| Kalorie aktywne | ❌ | ❌ | nie sprawdzono |
| Prędkość | ✅ | ❌ | nie sprawdzono |
| Moc, kadencja | ❌ | ❌ | nie sprawdzono |
| Przewyższenie | ❌ | ❌ | nie sprawdzono |
| Segmenty, okrążenia (długości basenu) | ❌ | ❌ | – |
| Trasa GPS | – | – | nie sprawdzono |

Źródłem wszystkich danych jest Health Sync (`nl.appyhapps.healthsync`). Do Health Connect nie dotarł żaden trening
innego typu niż basen i trenażer.

## Wnioski dla projektu
- Basen: nie planujemy tabeli odcinków ani długości (brak segmentów i okrążeń). Pokazujemy dystans, czas, kalorie, tętno i tempo /100 m liczone z dystansu i czasu.
- Trenażer: tylko czas, kalorie i tętno. Dystans i prędkość ukrywamy, bez zer.
- Kalorie: używamy łącznych (`TotalCaloriesBurned`), bo aktywnych brak.
- Rower na dworze: założenia z `PLAN.md` (trasa GPS, przewyższenie, prędkość) pozostają, ale pola są opcjonalne i ukrywane, jeśli ich brak. Do sprawdzenia, gdy będzie trening na dworze.

## Ograniczenie źródła danych
Darmowy Health Sync nie przenosi danych historycznych, więc w Health Connect są tylko ostatnie treningi.
Lokalna baza Room zapisuje wszystko, co Health Connect pokaże, dlatego synchronizacja w tle musi działać regularnie.
Starsze treningi spoza Health Connect zostają poza apką.

## Do zrobienia później
- Powtórzyć raport po jeździe rowerem na dworze z GPS (moc, kadencja, przewyższenie, trasa).
- Powtórzyć dla kolejnych pływań, żeby potwierdzić brak okrążeń.
