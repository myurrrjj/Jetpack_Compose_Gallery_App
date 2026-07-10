<div align="center">

# 🖼️ Jetpack Compose Gallery App

**A blazing-fast, modern, and intelligent local media gallery built entirely with Jetpack Compose.**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Dagger%20Hilt-DI-success.svg)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/Room-Database-lightgrey.svg)](https://developer.android.com/training/data-storage/room)
[![TensorFlow Lite](https://img.shields.io/badge/TFLite-On--Device%20ML-FF6F00?logo=tensorflow)](https://www.tensorflow.org/lite)

[Features](#-features) • [Tech Stack](#-tech-stack) • [Architecture](#-architecture) • [Getting Started](#-getting-started) 

</div>

---

## 📖 Overview

A fully featured native Android Gallery application designed for performance and aesthetics. It leverages the latest Android development standards to provide a seamless media viewing experience. The app intelligently organizes your photos and videos, offers smooth shared element transitions, and even features on-device machine learning for background face indexing and clustering.

---

## ✨ Features

### 🎨 Beautiful & Fluid UI
* **Material 3 Design:** Built fully on Compose with Material 3 components, dynamic theming, and an immersive edge-to-edge experience.
* **Shared Element Transitions:** Buttery smooth animations when navigating from the grid to the detail view using Compose's Experimental `SharedTransitionScope`.
* **Pinch-to-Zoom Grid:** Custom gesture detection allowing users to fluidly zoom in and out to switch between **Day, Month, and Year** grouping modes.
* **Contextual Action Bars:** Smart floating navigation and contextual selection bars that appear naturally based on scroll state and user intent.
* **Subsampling Scale Image View:** Deep, high-resolution zoom capabilities for large images without memory overhead.

### 🧠 Smart & Intelligent (On-Device ML)
* **Face Detection & Clustering:** Uses Google ML Kit to detect faces and a custom TensorFlow Lite model (`face_embedder.tflite`) to generate embeddings.
* **Background Processing:** Heavy ML lifting is offloaded to `WorkManager` (`FaceIndexingWorker`), ensuring the UI remains perfectly responsive while your library is indexed.

### 📼 Advanced Media Handling
* **Custom Video Player:** Integrated `ExoPlayer` with a sleek, custom-built Compose UI overlay featuring playback speed controls, dragging, and landscape toggles.
* **Optimized Image Loading:** Uses Coil combined with a custom `NativeThumbnailFetcher` that hooks directly into the Android MediaStore for instantaneous grid thumbnail loading.
* **Media Management:** Complete support for deleting, sharing (batch sharing included), favoring, and viewing detailed EXIF/metadata via a clean Bottom Sheet.

---

## 🛠️ Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Architecture:** MVI (Model-View-Intent)
* **Dependency Injection:** [Dagger Hilt](https://dagger.dev/hilt/)
* **Local Storage:** [Room Database](https://developer.android.com/training/data-storage/room) (for Favorites and Face Clusters)
* **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
* **Video Playback:** [ExoPlayer / Media3](https://developer.android.com/media/media3)
* **Machine Learning:** [Google ML Kit](https://developers.google.com/ml-kit) (Face Detection), [TensorFlow Lite](https://www.tensorflow.org/lite)
* **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
* **Media Retrieval:** `MediaStore` API

---

## 🏗️ Architecture

This project strictly follows the **MVI (Model-View-Intent)** architectural pattern to ensure predictable state management and unidirectional data flow.

```text
UI Layer (Compose)  --->  Dispatches Events (Intents)  --->  ViewModel
      ^                                                         |
      |                                                         v
Observes State  <---  Produces New State (Copy)  <---  Business Logic / Repositories
```

* **Contract (`GalleryContract.kt`):** Defines `GalleryState`, `GalleryEvent`, and `GalleryEffect`.
* **ViewModel (`GalleryViewModel.kt`):** Handles all logic, state mutations, and triggers side effects (like Navigation or Toasts).
* **Repositories (`MediaStoreRepositoryImpl.kt`):** Abstracts the complex `ContentResolver` and `MediaStore` queries into clean Kotlin Flows.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Iguana (or newer)
* JDK 17+
* Android SDK API Level 34

### Installation
1. Clone the repository:
   ```bash
   git clone [https://github.com/yourusername/JetpackComposeGallery.git](https://github.com/yourusername/JetpackComposeGallery.git)
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. **Important for ML:** Ensure the `face_embedder.tflite` model is placed in the `app/src/main/assets/` directory (the app expects this for the `FaceEmbedder` to initialize).
5. Build and run the app on a physical device (Emulators may struggle with hardware-accelerated video and ML clustering).

---

## 📂 Project Structure

```bash
com.example.jetpackcomposegalleryapp
│
├── core/               # Base MVI classes, Utility extensions, Custom Modifiers
├── core.ml/            # ML Kit Face Detector, TFLite Embedder, Cropper
├── data/               # Room DB, DAOs, Entities, MediaStore Implementation
├── di/                 # Dagger Hilt Modules (DB, ML, Coil, Media)
├── domain/             # Models (MediaAsset, Albums), Repository Interfaces
├── presentation/       # UI Layer (Screens, Components, ViewModel, Navigation)
├── ui.theme/           # Material 3 Color Schemes, Typography, Theme
└── worker/             # WorkManager classes for background face indexing
```

---

## 🤝 Contributing

Contributions, issues, and feature requests are always welcome!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 License

Distributed under the MIT License. See `LICENSE` for more information.

---
<div align="center">
  <b>Built with ❤️ using Jetpack Compose</b>
</div>
