# FrameFriends — iykyk Android Internship Assignment

FrameFriends is an entirely on-device Android app that processes portrait videos, detects faces, generates face embeddings, groups repeated appearances of the same person, selects a representative shot, and creates a shareable 9:16 collage.

## Features

- Selects portrait videos using Android’s document picker
- Samples video frames at 5 FPS
- Runs processing away from the main thread
- Detects faces using ML Kit
- Generates on-device MobileFaceNet embeddings
- Clusters repeated faces using cosine similarity
- Prevents faces visible in the same frame from being merged
- Counts continuous appearances using timestamp gaps
- Scores representative shots using pose, sharpness, eyes, smile, and clipping
- Generates a 1080 × 1920 collage
- Saves collages to the gallery
- Shares collages through the Android share sheet
- Uses no backend and uploads no frames or embeddings

## Tech stack

- Kotlin
- Jetpack Compose
- minSdk 26
- compileSdk 36
- Kotlin Coroutines
- Google ML Kit Face Detection
- LiteRT / TensorFlow Lite
- MobileFaceNet

## Embedding model

The repository bundles the model at:

```text
app/src/main/assets/mobile_face_net.tflite
```

Model source:

```text
https://github.com/MCarlomagno/FaceRecognitionAuth/blob/master/assets/mobilefacenet.tflite
```

Model details:

- Architecture: MobileFaceNet
- Runtime: LiteRT / TensorFlow Lite
- Input: float32 `[1, H, W, 3]`
- Input size is read dynamically from the model tensor
- RGB normalization: `(channel - 127.5) / 128`
- Output: float32 embedding vector
- Output embeddings are L2-normalized before comparison
- All inference runs locally on the Android device

## Build and run

### Requirements

- Android Studio
- JDK 17
- Android SDK 36
- Android 8.0 / API 26 or newer

### Steps

1. Clone the repository.
2. Open the repository root in Android Studio.
3. Use JDK 17 as the Gradle JDK.
4. Allow Gradle sync to complete.
5. Select a physical Android device or emulator.
6. Run the `app` configuration.

To generate the debug APK:

```text
Build → Generate App Bundles or APKs → Generate APKs
```

Generated APK location:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Processing pipeline

1. The selected video is opened using `MediaMetadataRetriever`.
2. Frames are sampled every 200 ms, equivalent to 5 FPS.
3. ML Kit detects faces and provides bounding boxes, tracking IDs, head pose, eye-open probabilities, and smile probability.
4. Tiny or substantially clipped detections are discarded.
5. Each accepted face is cropped and passed to MobileFaceNet.
6. MobileFaceNet creates an L2-normalized face embedding.
7. `IdentityClusterer` groups embeddings using cosine similarity.
8. `AppearanceTracker` counts continuous visible segments.
9. The highest-quality observation is selected for each person.
10. `CollageGenerator` creates a 1080 × 1920 collage.

All processing runs on-device. There is no backend.

## Identity grouping

`IdentityClusterer` performs online centroid-based clustering:

- Initial cosine-similarity threshold: **0.60**
- Near-duplicate cluster merge threshold: **0.495**
- Faces detected at the same timestamp are prevented from joining the same cluster
- Cluster centroids are recalculated after new observations are added

The same-frame restriction provides a generic cannot-link constraint because two different faces visible simultaneously cannot represent one identity.

## Appearance counting

An appearance is treated as one continuous visible segment. Observations are sorted by timestamp, and a new appearance begins when the gap between clear observations exceeds:

```text
650 ms
```

At 5 FPS, this tolerates short missed detections without immediately splitting one continuous appearance.

## Representative-shot scoring

Each candidate is scored using:

| Attribute | Weight |
| --- | ---: |
| Frontality from head Euler angles | 30% |
| Sharpness from edge variance | 25% |
| Eyes-open probability | 20% |
| Full-face visibility / clipping margin | 15% |
| Smile probability | 10% |

The highest-scoring observation becomes the representative shot. The final collage uses a generous portrait crop instead of tightly cropping the face bounding box.

## Test results

Testing was performed on a OnePlus Nord 5 using the three supplied portrait videos.

| Video | Unique people | Counted appearances |
| --- | ---: | ---: |
| Sample 1 | 5 | 16 |
| Sample 2 | 6 | 19 |
| Sample 3 | 5 | 17 |

The known Sample 1 target is five unique people and twenty appearances. The app consistently identifies five clusters on Sample 1. Very brief, blurred, overlapping, and rapid transition segments remain the primary source of appearance-count variance.

## Architecture

```text
MainActivity / Compose UI
        ↓
AppViewModel
        ↓
VideoProcessor
  ├── ML Kit FaceDetector
  ├── FaceEmbedder
  ├── IdentityClusterer
  ├── AppearanceTracker
  ├── ImageQuality
  ├── BitmapCrops
  └── CollageGenerator
        ↓
CollageExporter
```

`VideoProcessor.process()` runs using `Dispatchers.Default`. UI state and progress are exposed through `StateFlow`, keeping decoding, detection, and inference away from the main thread.

## Verification

- [x] Processed Samples 1, 2, and 3 without crashes
- [x] Sample 1 produced five unique-person tiles
- [x] Processing progress displayed in the UI
- [x] Face detection, embeddings, and clustering run on-device
- [x] Gallery saving verified
- [x] Android share sheet verified
- [x] Debug APK generated
- [x] Tested on a physical Android device
- [ ] Appearance counting can be improved for very brief transitions
- [ ] Representative selection can be improved when every observation in a cluster is blurred

## Performance trade-off

Sampling at 5 FPS produces approximately 150 frames for a 30-second video. This prioritizes short-appearance detection while keeping processing practical on a modern Android device.