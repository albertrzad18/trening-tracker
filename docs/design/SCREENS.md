# Ekrany do zaprojektowania

Dolna nawigacja: **Pulpit · Treningi · Statystyki · Więcej**.
Ekrany Podsumowania, Rekordy, Cele, Ustawienia i Diagnostyka otwierają się z „Więcej" (bez dolnego paska,
ze strzałką powrotu). Szczegóły treningu otwierają się kliknięciem w trening z dowolnego miejsca.

Każdy ekran ma trzy stany: **ładowanie**, **pusto**, **dane**. Dwa stany globalne (przed wejściem do apki):
brak Health Connect i brak uprawnień.

---

## 1. Pulpit (start)
Pionowa lista, przewijana. Zawartość od góry:

1. **Ten tydzień** — najważniejszy element ekranu:
   - łączny czas (np. `2:02:00`), pod spodem dystans i kalorie (`3,8 km · 1 027 kcal`),
   - zmiana względem poprzedniego tygodnia (`-37% vs poprzedni tydzień`), plus/minus ma być od razu czytelny.
2. **Trzy kafelki dyscyplin** obok siebie: nazwa, liczba treningów w tygodniu, dystans (jeśli jest).
   Kafelek dyscypliny bez treningów też jest pokazywany (liczba 0) — do rozstrzygnięcia w projekcie,
   czy to zostaje, czy ma wyglądać inaczej (wyszarzenie?).
3. **Ostatnie 7 dni** — słupki minut na dzień, kolorowane dyscypliną, dni bez treningu puste.
4. **Cele** — do dwóch kart z postępem, reszta pod linkiem. Karta celu: nazwa (dyscyplina · metryka · okres),
   „0,0 km z 300,0 km", pasek postępu, prognoza („W tym tempie wyjdzie 93,0 km"), ile dni zostało,
   historia ostatnich okresów (✔/✘) i seria. Sekcja znika, gdy nie ma żadnego celu.
5. **Nowe rekordy** — jeśli w ostatnich 7 dniach padł rekord: `🏆 Rower: Najdłuższy dystans`. Sekcja znika, gdy nie ma.
6. **Ostatnie treningi** — 3 karty: kropka dyscypliny, nazwa, data i godzina, czas, dystans.
7. Link **Zobacz podsumowania**.

Pusto: „Brak treningów" + zdanie wyjaśniające + przycisk **Synchronizuj teraz**.

## 2. Treningi (tabela)
- Na górze filtry: chipy dyscyplin (wielokrotny wybór) i chipy okresu (Tydzień / Miesiąc / Rok / Wszystko).
- Tabela z kolumnami: **data, dyscyplina, czas, dystans, kalorie, tempo/prędkość, śr. tętno**.
  Kliknięcie w nagłówek sortuje (strzałka ▲/▼ przy aktywnej kolumnie).
- Wiersze zgrupowane po miesiącach; nagłówek grupy ma nazwę miesiąca, liczbę treningów i sumy.
- Wiersz podsumowania przyklejony na dole: `Suma (299): 353 h 20 min · 5 141,4 km · 211 715 kcal`
  i średni czas treningu.
- Puste pola zostają puste (trenażer nie ma dystansu ani tempa).
- **Problem do rozwiązania w redesignie:** dzisiaj tabela ma stałą szerokość i przewija się w poziomie.
  Potrzebny układ, który mieści najważniejsze dane na szerokości telefonu (lista zamiast tabeli?
  mniej kolumn + szczegóły po kliknięciu? kolumny zależne od wybranych dyscyplin?).

## 3. Szczegóły treningu
1. Nagłówek: kropka i nazwa dyscypliny, data z godziną.
2. **Siatka metryk** (tylko te, które są): czas, dystans, kalorie, śr. tętno, maks. tętno,
   śr. prędkość (albo tempo dla basenu), maks. prędkość, przewyższenie.
3. **Tętno w czasie** — wykres liniowy, oś X w minutach od startu.
4. **Strefy tętna** — pięć stref z zakresem bpm i czasem w każdej (np. `Strefa 3 · 133+` → `1 h 15 min`).
   Strefy mają własne kolory od spokojnej do najcięższej. Dzisiaj to krzywe paski, wymaga przeprojektowania.
5. **Prędkość (km/h)** — wykres liniowy (brak dla basenu i zwykle dla trenażera).
6. **Trasa** — mapa OpenStreetMap ze śladem w kolorze dyscypliny, pod nią **profil wysokości**
   (oś X w kilometrach). Tylko rower na dworze.
7. **Na tle Twojej średniej** — `Czas: +55%`, `Dystans: +59%` względem średniej z innych treningów tej dyscypliny.

## 4. Statystyki
Na górze te same chipy co w Treningach (dyscyplina + okres). Pod nimi przewijana lista kart.
Każda karta ma tytuł, jedno zdanie wyjaśnienia i wykres:

1. **Objętość treningowa** — słupki warstwowe po dyscyplinach, z przełącznikiem **Czas / Dystans / Kalorie**.
   Dla okresu „Tydzień" i „Miesiąc" słupek to dzień, dla „Rok" i „Wszystko" — miesiąc.
2. **Kalendarz aktywności** — heatmapa 26 tygodni: kolumna to tydzień, kwadrat to dzień,
   kolor z dyscypliny, nasycenie z długości treningu. Pod spodem skala „mniej → więcej".
3. **Udział dyscyplin** — pierścień z legendą: `31% · 56 h 29 min`.
4. **Dni tygodnia** — słupki sumy czasu dla pon–niedz.
5. **Pora dnia** — słupki dla przedziałów `0–4`, `4–8`, … `20–24`.
6. **Dystans narastająco** — dwie linie: bieżący rok i poprzedni, oś X to miesiące.
7. **Trend: <dyscyplina>** — osobna karta dla każdej dyscypliny: cienka linia to pojedyncze treningi,
   gruba to średnia z pięciu. Basen pokazuje tempo /100 m (niżej = lepiej), rower i trenażer km/h.
8. **Forma: <dyscyplina>** — km/h na 100 uderzeń tętna, też linia + średnia krocząca.

Karty 7 i 8 mają stan „za mało danych": `Potrzebne są co najmniej dwa treningi z tymi danymi.`

## 5. Podsumowania
Zakładki **Tygodnie / Miesiące / Lata**, pod nimi lista kart okresów od najnowszego. Karta zawiera:
- nagłówek okresu (`19 paź 2026 – 25 paź 2026`, `październik 2026`, `2026`),
- sumy: `2 h 2 min · 3,8 km · 1 027 kcal`,
- liczbę treningów, dni z treningiem, najdłuższą serię dni,
- zmianę względem poprzedniego okresu (czas, dystans, kalorie),
- rozbicie na dyscypliny,
- najlepszy trening okresu (klikalny).

## 6. Rekordy
Karta na każdą dyscyplinę, w środku wiersze: nazwa rekordu, wartość, data (klikalne → trening).
Rekordy: najdłuższy dystans, najdłuższy czas, najlepsza średnia prędkość (rower/trenażer, od 20 km),
najlepsze tempo (basen, od 1000 m), najwięcej kalorii, największe przewyższenie.
Na końcu karta **Najlepsze okresy**: najlepszy tydzień i miesiąc pod względem dystansu i czasu.
Rekordy bez danych po prostu nie istnieją (trenażer ma zwykle tylko dwa).

## 7. Cele
Lista kart celów (jak na Pulpicie) + przycisk **Dodaj cel** na dole.
Okno dodawania: chipy dyscypliny (Wszystko / Basen / Rower / Trenażer), okresu (Tygodniowy / Miesięczny / Roczny),
metryki (Dystans (km) / Czas (godziny) / Kalorie / Liczba treningów) i pole liczbowe „Wartość celu".
Pusto: „Nie masz jeszcze żadnego celu. Dodaj pierwszy przyciskiem poniżej."

## 8. Więcej
Lista wejść: status synchronizacji (`Ostatnia synchronizacja: 24 paź 2026, 21:14`, ewentualny błąd),
przycisk **Synchronizuj teraz**, oraz odnośniki: Podsumowania, Rekordy osobiste, Cele, Ustawienia,
Diagnostyka danych z Health Connect. Dzisiaj to pięć identycznych przycisków jeden pod drugim.

## 9. Ustawienia
- **Tętno maksymalne** (pole liczbowe, domyślnie 190) + wyjaśnienie, po co jest.
- **Długość basenu**: 25 m / 50 m.
- **Motyw**: Jak w systemie / Jasny / Ciemny.
- Karta **Jak ustawić Health Sync** z instrukcją w pięciu krokach.

## 10. Diagnostyka (ekran techniczny)
Raport tekstowy o tym, co przyszło z Health Connect (liczby rekordów na typ danych), przycisk kopiowania
raportu i — tylko w wersji debug — przyciski danych demo. Może zostać surowy, ale powinien pasować do reszty.

## Stany globalne
- **Brak uprawnień:** zdanie wyjaśniające + przycisk „Nadaj uprawnienia".
- **Brak Health Connect:** zdanie + przycisk „Zainstaluj Health Connect".
- **Ładowanie:** dzisiaj kręciołek na środku.
Te trzy ekrany są pierwszym, co widzi nowy użytkownik — warto, żeby wyglądały jak powitanie, a nie jak błąd.
