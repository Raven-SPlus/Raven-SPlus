<div align="center">
  
# Raven S+
<p align="center">
    <img src="https://img.shields.io/github/issues/Raven-SPlus/Raven-SPlus?style=flat" alt="issues">
    <img src="https://img.shields.io/badge/license-GPLV3-green" alt="License">
</p>

[![Github Release Downloads](https://img.shields.io/github/downloads/Raven-SPlus/Raven-SPlus/total?label=Github%20Release%20Downloads&style=flat-square)](https://github.com/Raven-SPlus/Raven-SPlus/releases)

Raven B4, but for those who can't afford it.

Raven B4, but not only for Hypixel.

<a href="https://discord.gg/UT57ZW2TVB"><img src="https://invidget.switchblade.xyz/UT57ZW2TVB" alt="https://discord.gg/UT57ZW2TVB"/></a><br>

![menu](https://raw.githubusercontent.com/Raven-SPlus/Raven-SPlus/refs/heads/master/images/menu.png)
![gui](https://raw.githubusercontent.com/Raven-SPlus/Raven-SPlus/refs/heads/master/images/gui.png)
</div>

## How do I build this client?

### Steps:

1. **Download and Install Java SDK version 8**:
   - Download from [Java SDK version 8](https://adoptium.net/en-GB/temurin/releases/?version=8).
   - Install it and set the `JAVA_HOME` environment variable to the installation directory.

2. **Verify Java Installation**:
   - Open a terminal or command prompt.
   - Run `java -version`.
   - Ensure the output contains `openjdk version "1.8.0_{...}"`.

3. **Install Gradle 4.7**:
   - Download from [Gradle 4.7](https://gradle.org/next-steps/?version=4.7&format=bin).
   - Follow the "Installing manually" guide on the [Gradle wiki](https://gradle.org/install).

4. **Clone the Repository**:
   - Run the following commands in the terminal or command prompt:
     ```bash
     git clone --recursive https://github.com/Raven-SPlus/Raven-SPlus.git
     cd Raven-SPlus
     ```

5. **Set Up the Project**:
   - Run the following commands to set up the Gradle wrapper and the development environment:
     ```bash
     gradlew.bat
     gradle wrapper
     gradlew build
     ```

6. **Build Success**:
   - If all steps complete without errors, the project is successfully built.

## Credits
- *Powered by Cursor AI & OpenCode AI*
- RaidenyHK/Evolution1 (github.com/RaidenyHK/Evolution1): Matrix bypass code reference (I love Matrix and Matrix bypasses)
- Cryptix (Rest in Peace): Alt Manager auth flow, Hypixel InvMove and AutoBlock (trillionaire begs for unbans)
- Eclipse (github.com/corona-cn/eclipse): Polar & Grim Disabler, Matrix Speed, Entropy, Interpolation, mouse sensitivity GCD/mouse step quantization (Check CoronaCN this guy's a sick person)
- FDPClient (fdpinfo.github.io): Alt Manager OAuth login, mouse sensitivity GCD/mouse step quantization, rotation point selection, (reference) startup splash screen flow/guard
