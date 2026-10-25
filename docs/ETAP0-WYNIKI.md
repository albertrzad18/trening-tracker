# Etap 0: wyniki rozpoznania danych

Sprawdzone ekranem debug na telefonie, z danymi z Huawei Watch GT5 przez Huawei Health → Health Sync → Health Connect.
Raport zawiera tylko informację, jakie dane są obecne. Nie ma w nim wartości ani dat treningów.

**Próba jest mała:** 2 treningi basenowe, 1 rower na dworze i 1 trenażer (raport z ostatnich 365 dni). Wnioski do potwierdzenia na większej liczbie treningów.

## Co przechodzi

| Dana | Basen | Trenażer | Rower (dwór) |
|---|---|---|---|
| Tętno | ✅ | ✅ | ✅ |
| Dystans | ✅ | ❌ | ✅ |
| Kalorie łączne | ✅ | ✅ | ✅ |
| Kalorie aktywne | ❌ | ❌ | ✅ |
| Prędkość | ✅ (1 z 2) | ❌ | ✅ |
| Moc, kadencja | ❌ | ❌ | ❌ |
| Przewyższenie | ❌ | ❌ | ❌ (rekord ElevationGained) |
| Segmenty, okrążenia (długości basenu) | ❌ | ❌ | ❌ |
| Trasa GPS | – | – | jest, ale **wymaga zgody** (Health Connect: ConsentRequired) |

Trzy dyscypliny, które obsługuje apka, dotarły do Health Connect.

## Wnioski dla projektu
- Basen: nie planujemy tabeli odcinków ani długości (brak segmentów i okrążeń). Pokazujemy dystans, czas, kalorie, tętno i tempo /100 m liczone z dystansu i czasu.
- Trenażer: tylko czas, kalorie i tętno. Dystans i prędkość ukrywamy, bez zer.
- Kalorie: używamy łącznych (`TotalCaloriesBurned`), bo aktywnych brak.
- Rower na dworze: tętno, dystans, kalorie (łączne i aktywne) i prędkość są. Przewyższenia z Health Connect brak, więc sekcje z nim (siatka, profil wysokości) są ukryte. Trasa GPS istnieje, ale Health Connect wymaga osobnej zgody na odczyt trasy każdego treningu. Obecna synchronizacja jej nie prosi, więc mapa się nie pokaże (patrz niżej).

## Ograniczenie źródła danych
Darmowy Health Sync nie przenosi danych historycznych, więc w Health Connect są tylko ostatnie treningi.
Lokalna baza Room zapisuje wszystko, co Health Connect pokaże, dlatego synchronizacja w tle musi działać regularnie.
Starsze treningi spoza Health Connect zostają poza apką.

## Do zrobienia później
- **Trasa GPS roweru:** dodać w synchronizacji prośbę o zgodę na odczyt trasy (`ExerciseRouteResult.ConsentRequired`), inaczej mapa i profil wysokości nigdy się nie pojawią. Zmiana w warstwie `data`, do osobnego etapu.
- Sprawdzić na kolejnych jazdach, czy przewyższenie kiedykolwiek przychodzi (tu: 0/1).
- Powtórzyć dla kolejnych pływań, żeby potwierdzić brak okrążeń.
