# GravityChanger Project Guidelines

## Project Overview
GravityChanger is a Minecraft Fabric mod that allows changing gravity direction and strength for entities in Minecraft. It's a fork of FusionFlux's Gravity API, which itself was a fork of Gaider10's original GravityChanger mod. This version is maintained to ensure compatibility with the Immersive Portals mod, as Fabric mods cannot depend on Quilt mods.

The system supports arbitrary gravity in any direction, not just the six cardinal directions. It implements Oriented Bounding Boxes as an extension of the existing Minecraft AABB (Axis-Aligned Bounding Box) system to facilitate proper collisions, rendering, and other interactions when entities are affected by gravity in non-standard directions.

### Key Features
- Change gravity direction and strength for entities
- Gravity effects and potions
- Gravity anchor item that changes gravity when held
- Gravity plating that generates gravity fields with adjustable range
- Commands for manipulating gravity

## Project Structure
- `src/main/java/gravity_changer/` - Main source code
  - `api/` - Public API for other mods to interact with GravityChanger
  - `client/` - Client-side code
  - `collision/` - Collision handling with modified gravity
  - `command/` - Command implementation
  - `config/` - Configuration handling
  - `item/` - Items related to gravity changing
  - `mixin/` - Minecraft code modifications using Mixin
  - `mob_effect/` - Potion effects for gravity
  - `plating/` - Gravity plating blocks implementation
  - `util/` - Utility classes

## Dependencies
- Fabric Loader and API
- Cloth Config API
- ModMenu
- Cardinal Components API
- MixinExtras

## Development Guidelines
- The project uses Java 17
- Follow existing code style patterns when making changes
- When modifying gravity-related functionality, ensure compatibility with existing features
- Test changes with different gravity directions to ensure proper behavior
- Be careful with collision-related code as it's a complex area

## Building the Project
The project uses Gradle with Fabric Loom for building. To build the project:
1. Run `./gradlew build` (or `gradlew.bat build` on Windows)
2. The built JAR file will be in the `build/libs` directory

## Testing
When making changes, test the following scenarios:
- Different gravity directions (up, down, north, south, east, west)
- Gravity strength modifications
- Interaction with gravity-affecting items and blocks
- Entity behavior under modified gravity

Before submitting changes, ensure the project builds successfully and basic functionality works as expected.
