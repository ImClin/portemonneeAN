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

## Build & test
1. `./gradlew build`
2. De jar verschijnt in `build/libs/`
3. Plaats de jar in de server `plugins` map en start de server. Vergeet niet Vault/EssentialsX actief te hebben.