# Scapes
3D Voxel Engine written written in Kotlin, based on
[ScapesEngine](https://github.com/Tobi29/ScapesEngine).

The gameplay is implemented by plugins, like the VanillaBasics one found in
`Plugins/VanillaBasics`.

Designed to be powerful and easy to use, uses modern libraries and can support
any OpenGL binding that supports OpenGL 3.3. Utilizes a strict server + client
architecture ensure good multiplayer compatibility.


## Build
The project uses Gradle to build all modules.

In order to actually create any world, you need to also build the
`:Plugins:VanillaBasics` jar and add it to the game using the in-game Plugins
menu.


## Running
The build scripts can run the game without any OS specific preparing, by
executing the `run` task.

### Linux and MacOSX
  * Open terminal in project directory
  * Execute `./gradlew :run`

### Windows
  * Open cmd.exe and navigate to project directory
  * Execute `gradlew.bat :run`


## Deploy
You can check the available deployment targets using the `task` target (in
Deployment group).

Other than that, running the `deploy` target will run all available deploy
tasks.

Note: Windows deployment can take a long time due to compression, edit
`Setup.iss` to disable compression for testing.


## Dependencies
For simply compiling and running the game only a working JDK 8 is required,
all other dependencies are automatically downloaded by Gradle.

For deployment see the setup guide
[here](https://github.com/Tobi29/ScapesEngineBuild/blob/master/README.md)