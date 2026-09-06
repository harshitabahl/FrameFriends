# FrameFriends — iykyk Android Internship Assignment

An entirely on-device Android app that selects a portrait video, detects faces, generates face embeddings, groups repeated appearances of the same person, chooses a high-quality representative shot, and creates a 9:16 collage that can be saved or shared.

## Current status

The Kotlin/Compose project implements the complete processing path:

1. Video selection with Android's document picker
2. Frame sampling at 5 FPS on a background coroutine
3. Bundled ML Kit face detection with classification and tracking enabled
4. Face-quality scoring using head pose, sharpness, eye-open probability, smile probability and clipping margin
5. On-device TFLite embedding inference
6. Cosine-similarity clustering and near-duplicate-cluster merging
7. Timestamp-gap-based appearance counting
8. Representative-shot selection and generous portrait cropping
9. 1080×1920 collage generation
10. Gallery save and Android share sheet

No video frame or embedding leaves the device. There is no backend.

## Setup

### Prerequisites

- Android Studio (current stable)
- JDK 17 (Android Studio's bundled JDK is fine)
- Android SDK 36
- Android device/emulator running Android 8.0 / API 26 or newer

### Embedding model

This repository intentionally does not include third-party model weights until their redistribution licence is verified.

Add a MobileFaceNet TFLite model as:

```text
app/src/main/assets/mobile_face_net.tflite
```

Expected interface:

- Input: float32 `[1, H, W, 3]`; the app reads `H` from the tensor
- RGB normalization: `(channel - 127.5) / 128`
- Output: float32 `[1, N]`; commonly a 192-dimensional embedding
- The app L2-normalizes every output embedding before comparison

Before submission, record the exact model URL, licence, tensor sizes and SHA-256 here. A suitable audited source is required; do not submit weights copied from an unknown repository.

### Build

1. Open the repository root in Android Studio.
2. Allow Gradle sync to finish.
3. Add `mobile_face_net.tflite` as described above.
4. Run on a physical Android device for realistic video-processing performance.
5. Build the submission APK using **Build → Build APK(s)**.

If Android Studio asks for a Gradle wrapper, use its bundled Gradle once and run `gradle wrapper`, then commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/`.

## Identity grouping

`IdentityClusterer` performs online agglomerative grouping:

- Each observation is compared with every current cluster centroid using cosine similarity.
- It joins the closest cluster when similarity is at least **0.68**.
- Otherwise, it starts a new identity cluster.
- A second pass merges cluster centroids at **0.72** to recover identities split by pose or lighting changes.

The starting threshold is `0.68`; it must be calibrated on all three supplied videos. The final README should report the tested threshold and the observed counts for Samples 1–3.

## Appearance counting

An appearance is a continuous visible segment. Observations for an identity are sorted by timestamp. A new appearance begins when the gap from the preceding clear observation is greater than **650 ms**. At 5 FPS this tolerates up to two missed detections without splitting one continuous segment. Blurry/tiny/clipped detections are filtered before counting.

For Sample 1, the acceptance target is:

- 5 unique people
- 4 appearances per person
- 20 appearances total
- exactly 5 collage tiles

## Representative-shot selection

Each candidate receives a weighted score:

| Attribute | Weight |
| --- | ---: |
| Frontality from head Euler angles | 30% |
| Sharpness from an edge-variance estimate | 25% |
| Eyes-open probability | 20% |
| Full-face visibility / unclipped margin | 15% |
| Smile probability | 10% |

The highest-scoring observation becomes the representative. Collage tiles use a generous portrait crop around the face rather than the tight detector bounding box.

## Architecture

```text
MainActivity / Compose UI
        ↓
AppViewModel (state + coroutine lifecycle)
        ↓
VideoProcessor
  ├── ML Kit FaceDetector
  ├── FaceEmbedder (LiteRT)
  ├── IdentityClusterer
  ├── AppearanceTracker
  ├── ImageQuality / BitmapCrops
  └── CollageGenerator
        ↓
CollageExporter (MediaStore + share sheet)
```

`VideoProcessor.process()` runs in `Dispatchers.Default`. UI state is exposed through `StateFlow`, keeping all decoding and inference work off the main thread.
Full-resolution frames are released after each sampling step; only resized portrait candidates are retained to prevent memory pressure on 30-second videos.

## Testing checklist before submission

- [ ] Process Samples 1, 2 and 3 without crashes
- [ ] Confirm Sample 1 gives five identities and four appearances each
- [ ] Record Samples 2 and 3 expected counts after manual review
- [ ] Tune cosine threshold and appearance gap against all samples
- [ ] Inspect representatives for closed eyes, blur and clipping
- [ ] Confirm each identity appears exactly once in every collage
- [ ] Save collage and open it from the gallery
- [ ] Share collage through the Android share sheet
- [ ] Test portrait videos selected from Files and Google Drive
- [ ] Build and install the debug APK on a separate device
- [ ] Record a plain ≤60-second flow showing all three legible collages

## Known performance trade-off

Sampling at 5 FPS gives approximately 150 frames for a 30-second clip. This is deliberately accuracy-first for the supplied short videos. If processing is too slow on the test device, reduce to 4 FPS only after checking that short appearances are still detected.
