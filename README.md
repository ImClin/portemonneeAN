# PortemonneeAN

Kleine maar fijne plugin die ik voor onze Minetopia-achtige server heb gebouwd. Spelers kunnen speciale portemonnees vinden (via crates, pets, events – wat we maar willen) en daarmee een random bedrag claimen. Geen command-gedoe: gewoon de portemonnee in de hand houden, rechtsklikken om eerst het bedrag te zien en nogmaals klikken om het op je EssentialsX-bank te storten.

## Wat doet het?
- Genereert een willekeurig bedrag tussen het ingestelde minimum en maximum zodra een speler de portemonnee bekijkt.
- Laat meteen zien hoeveel erin zit (chat + aangepaste lore) zodat het een beetje spannend blijft.
- Stort het bedrag op de gewone server-economie (via Vault/EssentialsX) wanneer je opnieuw klikt.
- Houdt bij of een portemonnee al geclaimd is met behulp van de PersistentDataContainer, zodat dupes geen kans krijgen.

## Requirements
- Paper 1.21.4 (of iets dat compatibel is met Paper plugins).
- Vault + EssentialsX Economy (of een andere Vault-provider).
- ItemsAdder komt later voor het echte portemonnee-item/texture, voorlopig gebruiken we een nether star test-item.

## Configuratie
Alle teksten en bedragen staan in `config.yml`.
```yaml
wallet:
  min-amount: 1
  max-amount: 1000
  display-name: "&6Portemonnee"
  unrevealed-lore:
    - "&7Open de portemonnee om het geldbedrag te bekijken."
  revealed-lore:
    - "&7Je bekijkt de portemonnee en ziet &e€{amount}&7."
messages:
  reveal: "&9Je bekijkt de portemonnee en ziet &e€{amount}&9."
  ready-to-claim: "&7Klik nogmaals met de portemonnee om het geld te claimen."
  claimed: "&aJe hebt &e€{amount}&a geclaimd uit de portemonnee."
  claim-failed: "&cHet is op dit moment niet gelukt om te claimen. Neem contact op met een administrator."
  economy-unavailable: "&cHet economysysteem is niet beschikbaar. Neem contact op met een administrator."
  economy-error: "&cHet claimen is mislukt: {reason}"
  already-claimed: "&cDeze portemonnee is al geclaimd."
  not-revealed: "&cJe moet eerst het bedrag bekijken voordat je kunt claimen."
```

Tip: zodra ItemsAdder klaarstaat, vervang je het test-item in de command-code door het definitieve item. Tot die tijd kun je `/portemonnee give` gebruiken om te testen.

## Build & test
1. `./gradlew build`
2. De jar verschijnt in `build/libs/`
3. Plaats de jar in de server `plugins` map en start de server. Vergeet niet Vault/EssentialsX actief te hebben.

## TODO / ideeën
- ItemsAdder item + texture koppelen zodra we de definitieve asset hebben.
- GUI toevoegen voor het logboek van geclaimde portemonnees (handig voor staff).
- Misschien een admin-command om portemonnees in bulk te genereren.

Voor nu doet hij precies wat we nodig hebben: wallet vinden, bedrag zien, claimen en klaar. 🎉
