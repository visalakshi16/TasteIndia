# TasteIndia – Indian Recipe Discovery App

## About the Project

TasteIndia is an Android application developed to help users explore different Indian recipes. Users can browse recipes, search for dishes, filter them based on category and ingredients, view cooking instructions, and save their favourite recipes.

The app is developed using Kotlin and Jetpack Compose. It retrieves recipe information from the public TheMealDB API and uses Room Database to store favourite recipes locally.

## Features

- **Browse Recipes:** Displays recipes belonging to the Indian collection.
- **Search:** Search for recipes by entering their names.
- **Category Filter:** Filter recipes based on their category.
- **Ingredient Filter:** Find recipes based on the main ingredient.
- **Favourites:** Add recipes to or remove them from the favourites list.
- **Offline Access:** Previously saved favourites can be viewed without an internet connection.
- **Sorting:** Arrange recipes alphabetically from A–Z or Z–A.
- **Recipe Details:** View recipe images, ingredients, measurements, instructions, category, and other available information.
- **Filter Management:** View active filters, remove individual filters, or clear all filters.

## Technologies Used

| Technology | Usage |
|---|---|
| Kotlin | Android application development |
| Jetpack Compose | Creating the user interface |
| Material 3 | UI design and components |
| Navigation Compose | Moving between screens |
| ViewModel | Managing screen data and state |
| Kotlin Coroutines | Handling background operations |
| StateFlow | Managing and observing UI state |
| Retrofit | Connecting to the recipe API |
| OkHttp | Handling HTTP requests |
| Moshi | Converting JSON responses into Kotlin data |
| Room Database | Storing favourite recipes |
| Coil | Loading recipe images |
| KSP | Generating required code |
| JUnit | Writing unit tests |

## API Used

The application uses the public **TheMealDB V1 API** to fetch recipe information.

Base URL:

`https://www.themealdb.com/api/json/v1/1/`

The following API endpoints are used:

| Endpoint | Purpose |
|---|---|
| `filter.php?a=Indian` | Gets the Indian recipe collection |
| `lookup.php?i={mealId}` | Gets complete details of a recipe |
| `list.php?c=list` | Gets the available categories |
| `list.php?i=list` | Gets the available ingredients |
| `filter.php?c={category}` | Gets recipes belonging to a category |
| `filter.php?i={ingredient}` | Gets recipes containing an ingredient |

The application uses the public educational test key `1`. No additional API key setup is required.

## Project Structure

The project is organised into different packages to keep the code easy to understand and maintain.

```text
TasteIndia/
│
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/tasteindia/
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   ├── remote/
│       │   │   │   ├── MealRepository.kt
│       │   │   │   ├── FavoritesRepository.kt
│       │   │   │   └── Errors.kt
│       │   │   │
│       │   │   ├── domain/
│       │   │   │   └── Models.kt
│       │   │   │
│       │   │   ├── ui/
│       │   │   │   ├── components/
│       │   │   │   ├── recipes/
│       │   │   │   ├── details/
│       │   │   │   ├── favourites/
│       │   │   │   ├── navigation/
│       │   │   │   └── theme/
│       │   │   │
│       │   │   ├── MainActivity.kt
│       │   │   └── TasteIndiaApplication.kt
│       │   │
│       │   └── AndroidManifest.xml
│       │
│       └── test/
│           └── java/com/example/tasteindia/
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Application Architecture

The application is divided into three main layers.

**1. UI Layer**

This layer contains the Compose screens and reusable UI components. It displays recipe information and handles user interactions such as searching, filtering, sorting, and managing favourites.

**2. Domain Layer**

This layer contains the main data models used by the application, such as `MealSummary`, `MealDetail`, and `IngredientLine`.

**3. Data Layer**

This layer handles data retrieval and storage.

- The remote data source uses Retrofit to communicate with TheMealDB API.
- Moshi converts API responses into application data models.
- The local data source uses Room Database to store favourite recipes.
- Repositories manage the data received from the API and the local database.

ViewModels manage the state of individual screens and provide the required data to the UI. The application uses manual dependency injection through `AppContainer`.

## Recipe Filtering

One important part of the application is making sure that the displayed recipes belong to the Indian collection, even when users apply category or ingredient filters.

The filtering process works as follows:

1. The application first fetches the Indian recipe collection.
2. When a user selects a category, the corresponding recipe IDs are fetched from the API.
3. The same process happens when an ingredient is selected.
4. The application compares these IDs with the original Indian recipe IDs.
5. Only recipes that match the selected filters are displayed.

Search and favourites-only filtering are also applied to the recipe list. Finally, the results are sorted alphabetically.

If a user changes a filter while an earlier request is still running, the previous request is cancelled to avoid displaying outdated results.

## Screens in the Application

| Screen | Description |
|---|---|
| Recipes Screen | Displays recipes with search, category filters, ingredient filters, sorting, and favourite options. |
| Favourites Screen | Displays recipes saved by the user. Users can also remove recipes from this screen. |
| Details Screen | Displays the selected recipe's image, ingredients, measurements, instructions, and other available information. |

Navigation Compose is used to move between these screens. The application passes the recipe ID to the details screen instead of passing the entire recipe object.

## Local Storage and Offline Support

Room Database is used to save favourite recipes.

When a user marks a recipe as a favourite, its information is stored locally. The saved favourites remain available even after closing and reopening the application.

Users can view and remove saved favourites without an internet connection. However, complete recipe details are fetched from the API and are not stored for offline access.

## Error Handling

The application handles different situations that may occur while fetching recipe information.

- Internet connection failures.
- Network timeouts.
- Server and HTTP errors.
- Invalid API responses.
- Recipe details not found.
- Recipe image loading failures.

When an error occurs, the application displays an appropriate message and provides a retry option where applicable.

## State Management

ViewModels are used to manage the data and state of each screen.

StateFlow is used to send updates from ViewModels to the UI. The Compose screens collect these states using `collectAsStateWithLifecycle()`.

The application also restores search text, selected filters, and sorting preferences using `SavedStateHandle`. This helps retain the user's selections during screen recreation and process recreation.

## Testing

Unit tests are included to check important parts of the application.

| Test File | What It Checks |
|---|---|
| `MealMapperTest.kt` | Checks conversion of API responses into application models and ingredient handling. |
| `FilterLogicTest.kt` | Checks recipe searching, filtering, and sorting. |
| `RecipesViewModelTest.kt` | Checks ViewModel state updates, filter changes, and asynchronous responses. |

Fake repositories and sample JSON data are used during testing, so these unit tests do not require an internet connection.

## How to Run the Project

1. Clone or download this repository.
2. Open the project in Android Studio.
3. Wait for Gradle Sync to finish.
4. Start an Android emulator or connect an Android device.
5. Click the Run button to install and launch the application.

**Requirements:**
- Android Studio
- Compatible JDK and Android SDK
- Internet connection for fetching recipes and images

## Running Unit Tests

To run the unit tests on Windows, open the terminal and execute:

```bash
gradlew.bat testDebugUnitTest
```

For macOS or Linux:

```bash
./gradlew testDebugUnitTest
```

## Limitations

- An internet connection is required to fetch recipes and load complete recipe details.
- Favourite recipes are stored locally, but complete recipe details are not available offline.
- Users can select one category and one main ingredient at a time.
- Automated UI tests and Room database integration tests are not included.

## Credits

- Recipe information and images: [TheMealDB](https://www.themealdb.com/)
- UI development: [Jetpack Compose](https://developer.android.com/compose)
- Local storage: [Room Database](https://developer.android.com/training/data-storage/room)
- Networking: [Retrofit](https://square.github.io/retrofit/)

---

**TasteIndia – Explore Indian Recipes and Save Your Favourite Dishes!**
