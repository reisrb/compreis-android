# Compreis — Android

Shopping list app for Android built with Jetpack Compose and Room.

## Features

- **Shopping lists** — Create and manage multiple lists; mark a market name per list
- **In-progress mode** — Lists turn orange when you're actively shopping at a market
- **Item picking** — Tap items to confirm price at the market; checkbox marks them as picked
- **Price history** — Prices saved per product per market; suggests cheapest market alternative
- **Catalogue** — Browse all products with per-market prices sorted cheapest first
- **Report** — Monthly spending overview, spending by market, basket price comparison
- **Import / Export** — JSON backup and restore via the Profile screen
- **Localization** — English (default) and Brazilian Portuguese (pt-BR)

## Tech stack

| Layer | Tech |
|-------|------|
| UI | Jetpack Compose + Material 3 |
| State | ViewModel + StateFlow |
| Database | Room (SQLite) |
| Navigation | Navigation Compose |
| Language | Kotlin |
| Min SDK | 26 (Android 8.0) |

## Database schema

| Table | Key columns |
|-------|-------------|
| `shopping_lists` | id, name, marketName, inProgress, finalized |
| `items` | id, listId, name, price, unit, quantity, picked |
| `product_history` | name (PK), price, unit |
| `markets` | id, name |
| `market_prices` | id, productName, marketName, price, unit |

## Getting started

1. Clone the repo
2. Open in Android Studio
3. Run on an emulator or device (API 26+)

## Build

```bash
./gradlew assembleDebug
```

## Localization

Base language: **English** (`values/strings.xml`)  
Portuguese: `values-pt-rBR/strings.xml`

Add a new language by creating a `values-<locale>/strings.xml` file.
