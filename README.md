# Scapes
3D Voxel Engine written written in Java, based on
[ScapesEngine](https://github.com/Tobi29/ScapesEngine).

The gameplay is implemented by plugins, like the VanillaBasics one found in
`Plugins/VanillaBasics`.

Designed to be powerful and easy to use, uses modern libraries and can support
any OpenGL binding that supports OpenGL 3.2. Utilizes a strict server + client
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

Other than that, running the `deploy` target will run all available tasks.

Note: Windows deployment can take a long time due to compression, edit
`Setup.iss` to disable compression for testing.


## Dependencies
For simply compiling and running the game only a working JDK 8 is required,
all other dependencies are automatically downloaded by Gradle.

For deployment however additional software needs to be set up:

### Linux
  * `deployLinux32` and `deployLinux64` should be available out-of-the-box

### MacOSX
  * Unix environment highly recommended
  * Download a MacOSX (64bit, tar.gz version) JRE (Oracle JRE recommended)
  * Place the unextracted archive into `ScapesEngine/resources/JRE/MacOSX/`
  * Run `tasks` target to check if `deployMacOSX` is available

### Windows
  * Download a Windows (32bit + 64bit, tar.gz version)
    JRE (Oracle JRE recommended)
  * Place the unextracted archive into `ScapesEngine/resources/JRE/Windows/32`
    and `ScapesEngine/resources/JRE/Windows/64` respectively
  * Download Launch4j for your platform
  * Place the extracted archive into `ScapesEngine/resources/Launch4j`
    (Make sure the jar is in `ScapesEngine/resources/Launch4j/launch4j.jar`!)
  * Download Inno Setup (Unicode version recommended)
  * Windows only:
    * Run the installer and install everything into
      `ScapesEngine/resources/Inno Setup 5` (Make sure the compiler is in
      `ScapesEngine/resources/Inno Setup 5/ISCC.exe`!)
  * Non-Windows only:
    * Install Wine for your system (Running `wine --version` in the terminal has
      to work, as depends on that command to be set up)
    * Run the Inno Setup installer
    * Copy the `Inno Setup 5` directory to `ScapesEngine/resources/Inno Setup 5`
      (Make sure the compiler is in
      `ScapesEngine/resources/Inno Setup 5/ISCC.exe`!)
    * Make sure to have a working Wine prefix when building
  * Run `tasks` target to check if `deployWindows` is available
