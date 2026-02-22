# FuturoCraftMapGen

Plugin Paper/Spigot para gerar um mundo plano centralizado em `0,0,0` com:

- Área principal configurável (`map.size`, padrão `1000x1000`)
- Camadas:
  - Bedrock (base)
  - Stone com minérios aleatórios
  - Dirt
  - Grass Block na superfície
- Recursos de superfície:
  - Árvores aleatórias (vários tipos do Minecraft)
  - Laginhos
  - Pastos (grama alta e flores)
- Bordas em formato de praia (descendo para areia + água)

## Build

```bash
mvn clean package
```

Jar gerado em `target/futurocraft-mapgen-1.0.0.jar`.

## Configuração

Arquivo `config.yml`:

```yaml
world:
  name: "futuro_map"
  auto-create: true

map:
  size: 1000
  water-level: 58
  border-size: 80

layers:
  bedrock-y: 0
  stone: 10
  dirt: 4

features:
  tree-chance-per-chunk: 0.65
  lake-chance-per-chunk: 0.08
  pasture-chance-per-chunk: 0.20
```

> O spawn é ajustado para `0, surfaceY+1, 0`.
