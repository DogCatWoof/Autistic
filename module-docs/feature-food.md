# :feature:food

## Purpose
Everything food-related: logging meals, barcode scanning, photo-based food recognition via Claude Vision, nutrition lookup, and product caching.

## Functional Scope
- Log food items with nutrition data
- Scan barcodes to look up products (OpenFoodFacts, USDA FDC)
- Photograph meals for AI-assisted food identification (Claude Vision API)
- Cache food product data locally (ProductDatabase)
- View and manage food log history

## Key Files
- `ProductDatabase` — standalone Room database for cached food product data (separate from TaskDatabase)
- Food product entities and DAOs (all within this module)
- `FoodLogRepository` — CRUD for `FoodLogItemEntry` (uses `FoodLogItemDao` from `:core:database`)
- `FoodCacheRepository` — reads/writes cached product data in `ProductDatabase`
- `ProductRepository` — looks up products via remote APIs, falls back to cache
- `ClaudeVisionClient` — calls Claude API for photo-based food identification (API key injected via constructor)
- `FoodProductLookupService` — orchestrates barcode → OpenFoodFacts → USDA → cache lookup
- `ParsedNutritionData` — domain model for parsed nutrition info
- `OpenFoodFactsApiClient` — OpenFoodFacts REST client
- `UsdaFdcApiClient` — USDA FoodData Central REST client (API key injected via constructor)
- `FoodLogViewModel` — food log state
- `ScanViewModel` — barcode/photo scan state
- `FoodLogScreen` — food log list and entry UI
- `FoodLogPhotoDialogs` — photo capture and confirmation dialogs
- `ScanScreen` — barcode scan UI
- `ScanQueueList` — queue of scanned items pending log
- `CameraScreen` — camera composable for photo capture

## Dependencies
- `:core:database` (for `FoodLogItemDao` from TaskDatabase monolith)
- `:core:common`
- `:core:ui`

## Not In This Module
- `FoodLogItemEntity`/`FoodLogItemDao` — these are in `:core:database` as part of the TaskDatabase monolith
- Firestore sync (`:data:firestore`)
- Health Connect data (`:feature:health`)
