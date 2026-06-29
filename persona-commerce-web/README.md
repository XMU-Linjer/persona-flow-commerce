# PersonaFlow Commerce Web

V1.0 frontend demo built with Vue 3, Vite, Vue Router, Axios, TypeScript, Pinia, and Element Plus.

## Current Scope

Implemented:

- Base layout, navigation, login state, and logout
- Login and registration
- Product list, keyword search, pagination, and product detail
- SKU selection, add favorite, add cart item, and buy now
- Favorite list and remove favorite
- Cart list, quantity update, item delete, clear cart, and checkout entry
- Address list, create, edit, delete, and set default
- Checkout page and order creation
- Order list, status filter, order detail, cancel order, and mock payment
- Auth-required route guards

Not implemented in V1.0 frontend:

- Real third-party payment
- Refund, logistics, coupon, merchant system
- behavior, RabbitMQ, Agent, admin, RAG

## API Base URL

The backend defaults to `http://127.0.0.1:8080`.

Set `VITE_API_BASE_URL` if needed:

```sh
VITE_API_BASE_URL=http://127.0.0.1:8080
```

## Project Setup

```sh
npm install
```

## Development

```sh
npm run dev
```

If PowerShell blocks `npm.ps1`, use:

```sh
npm.cmd run dev -- --host 127.0.0.1
```

## Build

```sh
npm.cmd run build
```
