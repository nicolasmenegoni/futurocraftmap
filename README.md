# FuturoCraftMapGen

Plugin Paper/Spigot para gerar o mapa plano no mundo normal do servidor (ex.: `world`) centralizado em `0,0,0` com:

- Área principal configurável (`map.size`, padrão `1000x1000`)
- Camadas:
  - Bedrock (base)
  - Stone com minérios aleatórios
  - Dirt
  - Grass Block na superfície
- Recursos de superfície:
  - Árvores artificiais aleatórias
  - Laginhos
  - Pastos (grama alta e flores)
  - Casinhas simples com variações
- Bordas em formato de praia e mar:
  - Praia de areia (sem terra)
  - Faixa de mar maior configurável
  - Muro de vidro ao redor de todo o mapa (do bedrock até o topo)

## Build

```bash
mvn clean package
```

Jar gerado em `target/futurocraft-mapgen-1.0.0.jar`.

## Configuração

```yaml
world:
  name: "world"

map:
  size: 1000
  water-level: 58
  border-size: 140
  sea-width: 80
  glass-wall-height: 24

layers:
  bedrock-y: 48
  stone: 10
  dirt: 4

features:
  tree-chance-per-chunk: 0.95
  lake-chance-per-chunk: 0.25
  pasture-chance-per-chunk: 0.25
  house-chance-per-chunk: 0.07
```

> O spawn é ajustado para `0, surfaceY+1, 0`.


> O plugin **não cria outro mundo**; ele aplica o gerador no mundo já existente configurado em `world.name`.

> Ao entrar/respawnar, o jogador é reposicionado no spawn seguro do mapa (0,0) para evitar nascer no vazio.

> O plugin gera o mapa inteiro no startup (todos os chunks da área configurada), evitando atraso ao entrar.

> Jogadores nascem em posições aleatórias nas praias (areia), sempre dentro da área do mapa.

> Árvores são criadas conforme biomas do mapa (floresta, birch, taiga, jungle, savanna e cherry grove).
> Animais aparecem espalhados, porém raramente.
