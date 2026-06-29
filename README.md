# EinkArcade

EinkArcade is a Kotlin/Android prototype of **Sokobanitron**, a Sokoban game designed for e-ink displays. The main Sokobanitron project is implemented in Rust and lives in a separate repository.

This prototype explores a high-contrast, grayscale interface with tap-to-move pathfinding instead of directional controls. Its Android app label remains **Sokobanitron**.

![Sokobanitron gameplay](docs/gameplay.png)

## Features

- Tap a reachable square to move the player there automatically.
- Select a box, then tap its destination to plan and animate a valid push path.
- Undo moves with Android's back action, restart a changed level, or skip to the next puzzle.
- Browse level sets and individual puzzles.
- Like, dislike, and star puzzles.
- Persist puzzle metadata, completion state, and best solutions locally with Room.
- Synchronize levels and progress with a remote HTTP endpoint.
- Animate box movement, completed boxes, rejected moves, and transitions between levels.

## Requirements

- Android Studio with Android SDK 36 installed
- JDK 11 or newer
- An Android 13+ device or emulator (API 33 is the minimum)
- A compatible level-sync server for a fresh install

## Getting started

1. Clone the repository and open it in Android Studio.
2. Set the sync server URL in `app/src/main/java/com/example/einkarcade/data/LevelsRepository.kt`. The checked-in value is a private-network development address:

   ```kotlin
   private const val DEFAULT_SYNC_ENDPOINT = "http://192.168.0.75:8000/api/sync"
   ```

3. Start the sync server and make sure it is reachable from the Android device. For a server running on the development machine, an Android Emulator normally reaches the host through `10.0.2.2` rather than `localhost`.
4. Build and install the debug app:

   ```shell
   ./gradlew installDebug
   ```

   You can also select the `app` run configuration in Android Studio and click **Run**.

On first launch, the app populates its Room database from the sync endpoint. Startup fails if the database is empty and the server is unavailable or returns no level sets. After the initial sync, the locally stored catalog is available without bootstrapping again.

## Playing

- Tap an open board square to walk there.
- Tap a box to select it, then tap a destination to push it along a valid route.
- Use the back gesture or button to undo the last move.
- Tap the level name at the top left to choose a puzzle.
- Tap the level-set name at the top center to choose or synchronize a set.
- Use the star, heart, and trash icons to save and rate puzzles.
- Use the side controls to restart or skip a puzzle.

## Sync API

The app sends a `POST` request to `/api/sync` with local puzzle progress:

```json
{
  "puzzles": [
    {
      "puzzle_id": 1,
      "rating": 0,
      "is_starred": false,
      "last_completed_at": null,
      "user_solution": null
    }
  ]
}
```

The server response must contain `level_sets`, `levels`, and `puzzles` arrays. The client replaces its local catalog with the returned data while retaining the progress fields supplied by the server.

Cleartext HTTP is enabled for development. Use HTTPS and revise the Android network policy before distributing the app.

## Development

Run the JVM unit tests:

```shell
./gradlew test
```

Run the instrumented UI tests on a connected device or running emulator:

```shell
./gradlew connectedDebugAndroidTest
```

Create debug and release builds:

```shell
./gradlew assembleDebug
./gradlew assembleRelease
```

The main implementation areas are:

- `sokoban/` — board model, movement rules, and player/box pathfinding
- `ui/` — Compose overlays plus the custom Android game-board renderer and animations
- `data/` — Room persistence and HTTP synchronization
- `catalog/` and `selection/` — level browsing and default-level selection

## Technology

Kotlin, Jetpack Compose, a custom Android `View`/Canvas renderer, Room, KSP, and JUnit. The build uses Gradle 8.14.2 and Android Gradle Plugin 8.13.2.
