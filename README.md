# WordVenture – Android Word Puzzle Game

## Table of Contents
- [WordVenture](#wordventure)
- [Design Philosophy & Considerations](#design--philosophy--&--considerations)
- [Overview](#overview)
- [Features](#features)
- [Gameplay](#gameplay)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [GitHub Actions & CI/CD](#github-actions--cicd)
- [Installation & Setup](#installation--setup)
- [API Integration](#api-integration)
- [Screens & UI](#screens--ui)
- [Release Notes & Innovative Features](#release--notes--&--innovative--features)
- [Future Improvements](#future-improvements)
- [Contributors](#contributors)

---

## WordVenture

**WordVenture** is a word-based puzzle game designed to challenge players’ problem-solving abilities. Players must guess randomised 5-letter words while exploring multiple game modes, personalisation features, and continuous gameplay designed to keep them engaged and entertained.  

The application’s design is modern and innovative, while still maintaining simplicity and accessibility.  
What sets **WordVenture** apart from the traditional Wordle experience is the introduction of **continuous play through progressive levels**, while still including the popular daily challenge to encourage community engagement and shared experiences.

---

## Design Philosophy & Considerations
The core design of WordVenture was driven by three main principles: engagement, scalability, and accessibility.

**1. User Experience**
- Intuitive Feedback: the colour-coded feedback system (green, yellow, grey) is instantly recognisable and was used to minimise the learning curve.
- Consistent Flow: the application's navigation, moving from secure **Login/Register** to the **Main Menu** and into specific game modes, was designed to be clear and easy to use.
- Persistent Customisation: features like **Background Music control** and **Profile Management** ensure the game adapts to the user’s preferences.

**2. Technical Architecture**
- Testability: core game logic is separated from the Android UI, implemented as plain Kotlin classes (e.g., WordleLogic.kt) for ease of unit testing.
- Modularity: the application utilises a dedicated Firebase Cloud Messaging Service to handle push notifications separately, ensuring clean communication logic.
- API Decoupling: reliance on a Node.js API ensures that dynamic content (Daily Word) is managed server-side, allowing the mobile application to focus on the UI/UX.

---

## Overview

**WordVenture** transforms the familiar Wordle-style puzzle into a dynamic and evolving word adventure. The app aims to provide a fun yet mentally challenging experience by combining the simplicity of word guessing with progressive gameplay, statistics, and personalisation options.

Players can enjoy quick daily challenges, dive into continuous level-based play, or compete against each other in multiplayer modes — all while tracking their progress and customising their experience.

---

## Features

### Core Gameplay
- **Daily Word Challenge:** Solve a new 5-letter word puzzle each day, the same for all players.  
- **Continuous Play Mode:** Play unlimited puzzles in progressive levels.  
- **Multiplayer Mode:** Challenge friends locally or online to a match.

### Player Experience
- **Real-Time Feedback:** Colour-coded feedback for each guess (green, yellow, grey).  
- **Statistics:** Track win streaks, games played, and guess distribution.
- **Daily Push Notifications:** receive server-triggered reminders via Firebase Cloud Messaging (FCM) when the new Daily Word is ready.

### Account & Customisation
- **Secure Accounts:** Register and log in with Firebase authentication.
- **Biometric Authentication:** Log in using fingerprint / face ID. 
- **Customisable UI:** Manage in-game audio.  
- **Profile Management:** Choose an avatar profile picture and update your username easily.
- **Multi-Language Support:** Change the app language in the **Settings Page**.

---

## Gameplay

- **Objective:** Guess the 5-letter word in 6 attempts.  
- **Feedback System:**  
  - 🟩 **Green:** Letter is correct and in the right position.  
  - 🟨 **Yellow:** Letter exists but in the wrong place.  
  - ⬜ **Grey:** Letter is not in the word.  
- **Modes:**  
  - **Daily:** New word each day.  
  - **Continuous Play:** Unlimited puzzles.  
  - **Multiplayer:** Compete against your friends locally or online.

---

## Architecture

**WordVenture** follows a modular architecture designed for scalability, security, and performance.

|Component|Technology|Purpose|
|---|---|---|
|**Frontend**|Android Native (Kotlin/Java)|Core application logic and User Interface|
|**Backend API**|Node.js / Express|Provides daily words, validation, and statistics|
|**Database**|Firebase|Secure user authentication and cloud data storage|
|**Messaging**|Retrofit|Type-safe HTTP client for communication with the Node.js API|
|**Messaging**|Firebase Cloud Messaging (FCM)|Handles server-triggered Daily Push Notifications|
---

## Tech Stack

- **Languages:** Kotlin / Java  
- **Framework:** Android Native  
- **Backend:** Node.js  
- **Database:** Firebase  
- **Authentication:** Firebase Auth  
- **Hosting:** Render (API Hosting)  
- **Version Control:** GitHub  
- **Automated Testing:** Unit tests & GitHub Actions

---

## GitHub Actions & CI/CD

Throughout development, **GitHub** was used for version control, collaboration, and automation. We implemented **GitHub Actions** to enable Continuous Integration (CI) and Continuous Deployment (CD), which provided the following benefits:

- **Automated Builds:** Push and pull requests triggered a build pipeline to ensure the project compiled successfully.  
- **Unit Testing:** Tests were automatically executed on every commit, ensuring new changes did not break existing functionality.  
- **Code Quality Assurance:** GitHub workflows helped maintain a clean and reliable codebase.

This integration streamlined development and ensured consistent quality across the project lifecycle.

---

## Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/ST10036346/WordVenturePROG.git
   ```
2. **Open in Android Studio**
3. **Configure Firebase**
  - Create a new Firebase project and add the ```bashgoogle-services.json``` file to the ```/app``` directory.
4. **Run the App**
  - Connect an Android device or start an emulator.
  -	Click Run in Android Studio.
---

## API Integration
WordVenture connects to a custom Node.js backend API for dynamic content:
| Endpoint |	Method |	Description |
|----------|---------|--------------|
| /daily-word |	```GET``` |	Fetches the daily 5-letter word |
| /guess |	```POST``` |	Validates a user’s guess |
| /users/:userId/fcm-token | ```POST``` | Registers the device's FCM token for receiving targeted daily notifications |
---

## Screens & UI
- Opening Page: displays WordVenture logo and options to register or login.
- Login / Register: secure authentication with email/password and SSO.
- Main Menu: access Daily, Continuous Play, and Multiplayer modes.
- Game Screen: interactive grid with live feedback and virtual keyboard.
- Profile & Settings: edit profile, manage audio, and contact help & support.
---

## Release Notes & Innovative Features
#### Version 1.0.0 - Initial Release (The WordVenture Prototype)
This release establsihed the foundation of the game, focusing on core functionality and crucial innovative features that distinguish **WordVenture** from standard word games.

|Feature|Innovative Contribution|
|---|---|
|**Continuous Level Play**|The core differentiator: provides unlimited gameplay that moves beyond the single-daily-word limit to maximise player retention|
|**Server-Triggered Daily Push Notifications**|Implemented Firebase Cloud Messaging (FCM) to send targeted "New word is ready!" notifications, significantly increasing daily user engagement|
|**Decoupled Game Logic & Stats**|The game logic and dedicated Statistics Manager (```StatsManager.kt```) were separated from the UI for easy unit testing and future feature expansion, ensuring a scalable foundation|
|**Android 13+ Notification Permissions**|Implemented the required runtime permission check (```POST_NOTIFICATIONS```) for a modern, professional Android user experience|
|**Automated Unit Testing CI**|Implemented GitHub Actions to automate builds and test runs on every commit, ensuring code stability and quality assurance across the development team|
|**Retrofit API Integration**|Established type-safe network calls to the Node.js API using RetrofitClient.kt, ensuring reliable and efficient fetching of dynamic daily content|
---

## Contributors
- Emma Mae Atkinson 
- Ayushkar Ramkissoon 
- Ethan Pillay

