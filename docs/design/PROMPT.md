# Prompt do Claude (redesign apki trening-tracker)

Skopiuj tekst poniżej i wklej do Claude razem z plikami z listy na końcu.

---

Jesteś projektantem produktu i projektujesz **kompletny redesign** istniejącej apki na Androida.
Apka nazywa się **trening-tracker**. Pokazuje treningi z zegarka Huawei Watch GT5: pływanie na basenie,
rower na dworze i trenażer. Działa tylko na telefonie, bez konta i bez serwera. Używa jej jedna osoba
(właściciel) i ewentualnie kilku znajomych.

**Stan dzisiaj:** apka jest w pełni zbudowana i działa, ale wygląda jak zlepek domyślnych komponentów
Material 3 — same szare karty jedna pod drugą, brak hierarchii, brak charakteru. Zrzuty ekranu są w załącznikach.
Treść i funkcje zostają, zmienia się **wygląd i układ**.

## Co masz zrobić

1. **Kierunek wizualny** (jeden, nie trzy warianty): nastrój, paleta, typografia, kształty, cienie, siatka,
   gęstość informacji, sposób pokazywania liczb i wykresów. Napisz krótko, dlaczego taki, a nie inny.
2. **System projektowy**: tokeny kolorów (jasny i ciemny), skala typograficzna, odstępy, promienie,
   stany komponentów. Nazwij komponenty tak, żeby dało się je przełożyć na Compose.
3. **Makiety ekranów** (wszystkie z `SCREENS.md`): Pulpit, Treningi, Szczegóły treningu, Statystyki,
   Podsumowania, Rekordy, Cele, Więcej, Ustawienia, Diagnostyka, plus stany: ładowanie, pusto, brak uprawnień,
   brak Health Connect.
4. **Zasady dla wykresów**: jak mają wyglądać słupki, linie, heatmapa, pierścień i mapa trasy, żeby
   wszystkie wyglądały jak jedna rodzina.
5. **Spis zmian względem obecnej wersji**: co wylatuje, co dochodzi, co zostaje.

Makiety zrób jako interaktywny artefakt (HTML + CSS) w rozmiarze telefonu (390×844), z przełącznikiem
jasny/ciemny. Użyj **prawdziwych treści z `CONTENT.md`** — polskie teksty, polskie formaty liczb,
realne wartości. Żadnych „Lorem ipsum" i żadnego angielskiego w interfejsie.

## Twarde ograniczenia (nie do ruszenia)

- **Interfejs po polsku.** Formaty: `1 234,5 km`, `1:23:45`, `24 paź 2026`, `2:15 /100 m`, `+12%`.
- **Stałe kolory dyscyplin**: basen niebieski, rower zielony, trenażer pomarańczowy. Możesz zmienić
  konkretne odcienie, ale podział kolorów na dyscypliny zostaje i musi działać w obu motywach.
- **Jasny i ciemny motyw**, oba równie dopracowane.
- **Brakujące dane ukrywamy, nie pokazujemy zer.** Trening na trenażerze nie ma dystansu i nie ma
  w tym miejscu pokazywać „0 km" ani pustego miejsca po stole.
- Technologia: **Jetpack Compose + Material 3**. Projektuj tak, żeby dało się to zbudować standardowymi
  komponentami albo prostym własnym rysowaniem. Bez efektów wymagających silnika 3D czy wideo.
- Wykresy rysuje biblioteka Vico (słupki, linie) i własny `Canvas` (heatmapa, pierścień), mapa to
  OpenStreetMap przez osmdroid. Projekt ma się w tym zmieścić.
- Apka musi działać jedną ręką na telefonie i czytelnie wyglądać na małym ekranie (360 dp szerokości).

## Czego chcemy od strony odczuć

Trzy słowa: **nowoczesne, płynne, minimalistyczne.**

- **Nowoczesne** znaczy: przemyślana typografia i odstępy, a nie modne ozdobniki. Ma wyglądać jak
  narzędzie zrobione przez kogoś, kto wie, co robi, a nie jak szablon ze sklepu z motywami.
- **Płynne** znaczy: spokojne przejścia między ekranami, wykresy, które dorysowują się przy wejściu,
  reakcja na dotyk bez skakania układu. Animacja ma tłumaczyć zmianę, a nie zwracać na siebie uwagę.
  Żadnych odbić, sprężyn i efektów dłuższych niż ułamek sekundy.
- **Minimalistyczne** znaczy: mało elementów, dużo powietrza, jeden najważniejszy wskaźnik na ekranie.
  Duże liczby, wyraźna hierarchia, czytelność jednym rzutem oka po treningu, często przy słabym świetle.
  Jeśli coś nie niesie informacji, ma zniknąć.

### Czego nie chcemy (to jest ważne)

Projekt **nie może wyglądać jak typowy wygenerowany interfejs**. Konkretnie, nie używaj:

- **emoji w interfejsie** — żadnych 🏆 🎉 🔥 💪 ani znaczków w rodzaju ✔/✘ wstawianych jako ikony.
  Jeśli coś ma mieć symbol, to ma to być ikona z jednego, spójnego zestawu (Material Symbols
  albo własny), w kolorze z palety;
- fioletowo-niebieskich gradientów, poświat, szkła (glassmorphism) i cieni dokładanych „dla efektu";
- stosu identycznych zaokrąglonych kart jedna pod drugą (to jest dokładnie dzisiejszy problem);
- napompowanych nagłówków typu „Twoje niesamowite postępy" i haseł motywacyjnych;
- dekoracyjnych ilustracji, maskotek, ikon-naklejek, losowych akcentów koloru;
- trzech odcieni szarości tam, gdzie wystarczy jeden kontrast.

Dekoracja ma być efektem ubocznym dobrej kompozycji, nie osobną warstwą doklejoną na wierzch.
Kolor zostaw danym: dyscyplinom, wykresom i jednemu akcentowi na akcję. Reszta to tło i tekst.

**Uwaga:** w obecnej apce jest kilka emoji (`🏆` przy nowych rekordach, `🎉` przy zrobionym celu,
`✔`/`✘` w historii celu). Zaprojektuj, co wchodzi w ich miejsce.

## Jak chcę to dostać

1. Najpierw **jeden akapit** z kierunkiem i jedna tabela tokenów (kolory, typografia, odstępy, promienie).
2. Potem makiety wszystkich ekranów w artefakcie.
3. Na końcu lista zmian i lista rzeczy, które trzeba będzie rozstrzygnąć w kodzie.

Zadaj mi pytania tylko wtedy, gdy czegoś naprawdę nie da się wywnioskować z załączników.
W pozostałych przypadkach podejmij decyzję i napisz, dlaczego.

## Załączniki

- `BRIEF.md` – kontekst produktu, użytkownik, zasady.
- `SCREENS.md` – wszystkie ekrany, co jest na nich i jakie mają stany.
- `CONTENT.md` – prawdziwe teksty i przykładowe dane do makiet.
- `docs/screenshots/*.png` – jak to wygląda dzisiaj.
