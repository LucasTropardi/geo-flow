# geoflow-front

Frontend do GeoFlow construído com React, TypeScript e Vite, permite criar um job `BBOX_TO_GEOJSON`, aguardar a conclusao do processamento via SSE e visualizar o resultado final em mapa e em JSON.

## O que foi feito

- formulario para criar jobs com dois cantos opostos de uma area
- normalizacao de `min` e `max` no proprio frontend antes do envio
- pagina de espera conectada ao notifier via `EventSource`
- pagina de resultado com:
  - status final do job
  - correlationId
  - contagem de tentativas
  - renderizacao do GeoJSON com Leaflet
  - exibicao formatada do GeoJSON
  - historico de logs retornado pela API
  - botao de reprocessamento quando o job falha

## Stack

- React 19
- TypeScript 6
- Vite 8
- Axios
- React Router DOM 7
- Leaflet + React Leaflet

## Rotas da aplicacao

- `/`: formulario de criacao do job
- `/jobs/:jobId/waiting`: tela de espera conectada ao SSE
- `/jobs/:jobId/result`: tela de resultado

## Integracoes

Durante o desenvolvimento, o Vite usa proxy para os servicos backend:

- `/api` -> `http://localhost:8080`
- `/notifications` -> `http://localhost:8082`

Chamadas usadas:

- `POST /api/jobs`
- `GET /api/jobs/{jobId}`
- `GET /api/jobs/{jobId}/logs`
- `POST /api/jobs/{jobId}/reprocess`
- `GET /notifications/jobs/{jobId}/stream`

## Como rodar

```bash
npm install
npm run dev
```

Por padrao, o Vite sobe em `http://localhost:5173`.

## Scripts disponiveis

- `npm run dev`: ambiente de desenvolvimento
- `npm run build`: build de producao
- `npm run lint`: lint do projeto
- `npm run preview`: preview local do build

## Fluxo do usuario

1. O usuario preenche `tenantId`, nome do job e dois pares de latitude/longitude.
2. O frontend converte os valores para numero e monta a area normalizada.
3. A aplicacao envia um `POST /api/jobs` com `jobType: BBOX_TO_GEOJSON`.
4. Ao receber o `id`, a navegacao vai para `/jobs/{id}/waiting`.
5. A tela de espera abre uma conexao `EventSource` no notifier.
6. Quando chega o evento final, a aplicacao redireciona para `/jobs/{id}/result`.
7. A tela final consulta job e logs na API e renderiza o resultado.

## Observacoes

- o frontend espera que o notifier envie eventos SSE chamados `subscribed` e `job-status`
- se o job terminar com falha, a tela de resultado mostra `errorMessage`
- se o job terminar com falha, a tela tambem permite solicitar reprocessamento manual
- se houver `resultGeojson`, o mapa e o JSON sao exibidos

## Estrutura relevante

```text
src/
├── App.tsx
├── App.css
├── index.css
├── main.tsx
└── assets/
```
