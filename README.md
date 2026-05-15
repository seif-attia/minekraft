# MineKraft Voxel Engine 

A fully custom, infinite-generation 3D voxel game engine built from scratch in Java using the [jMonkeyEngine](https://jmonkeyengine.org/). 

MineKraft was developed to explore advanced rendering optimizations, custom physics implementations, and procedural generation within a modular architecture.

<img width="1600" height="854" alt="WhatsApp Image 2026-05-12 at 9 15 07 AM" src="https://github.com/user-attachments/assets/72179a52-2a6d-41b8-a641-ae2674e48022" />
<img width="1600" height="863" alt="WhatsApp Image 2026-05-12 at 9 15 22 AM" src="https://github.com/user-attachments/assets/286770fa-d24e-47fd-ab2c-b8b0aba18db3" />
<img width="1600" height="860" alt="WhatsApp Image 2026-05-12 at 9 16 19 AM" src="https://github.com/user-attachments/assets/53a5a82b-8284-48e3-af86-fd87c8b71e60" />



##  Key Features

###  Infinite Terrain Generation
* **Dynamic Chunk Loading:** The world is divided into 16x16x384 voxel chunks that load and unload asynchronously based on player render distance.
* **Multi-threaded Architecture:** A background `ExecutorService` handles heavy procedural generation math to ensure the main game loop maintains a high framerate.

###  Rendering & Optimization
* **Greedy Meshing:** Radically optimizes GPU performance by mathematically combining hundreds of adjacent block faces into massive single polygons, dropping vertex counts from millions to thousands.
* **Smart Face Culling:** Automatically deletes hidden geometry (e.g., buried dirt blocks) to save processing power.
* **Advanced Transparency:** Seamless visual intersections between solid terrain, water, foliage, and glass without exposing the skybox.

###  Custom Physics & Mechanics
* **AABB Intersection Safety:** Axis-Aligned Bounding Box math prevents players from accidentally suffocating by placing blocks inside their own hitbox.
* **Precision Raycasting:** Real-time 3D raycasting calculates the exact voxel targeted for precise block breaking and placing.
* **Ghost Mode:** Toggleable free-flight spectator mode with disabled collisions for safe exploration and debugging.

###  Atmospheric Graphics
* **Volumetric Light Scattering:** Post-processing "God Rays" that dynamically bleed through trees and mountain peaks.
* **Real-time Shadows:** Directional sun mapping with edge-filtered shadows.
* **Environmental Polish:** Distance fog for smooth chunk blending, sun bloom, drifting cloud layers, and ambient floating dust particles.

###  Dynamic UI & HUD
* **Resolution-Independent GUI:** Built with the Lemur framework using dynamic screen-percentage math to scale flawlessly on any monitor.
* **Real-Time Minimap:** An orthographic viewport masked cleanly into the HUD tracks player position live.
* **Interactive Hotbar:** A fully functional 9-slot inventory system mapped to physical block IDs.
* **Developer Metrics:** Built-in settings for live FPS tracking and wireframe mode to visualize greedy meshing in real time.

---

##  How to Play

You do not need to install Java to play this game! It comes bundled with its own runtime.

1. Navigate to the **[Releases](../../releases)** tab on the right side of this repository.
2. Download the latest `.zip` file.
3. Extract the entire `.zip` folder. *(Do not run the executable from inside the zip!)*
4. Double-click `Play_MineKraft.exe` to launch the game.

## Controls
* WASD for Movement
* X to turn on WireFrame Mode
* C to turn Camera Mode on and off
* Left Mouse Click to destroy blocks
* Right Mouse Click to place blocks
* 0-9 or Mouse Scroll Wheel to select items from the hotbar

---

##  Building from Source

If you want to explore the code, modify the engine, or build it yourself:

### Prerequisites
* Java Development Kit (JDK) 21
* jMonkeyEngine SDK (or your preferred Java IDE like IntelliJ with Gradle)

### Setup Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/seif-attia/minekraft.git
