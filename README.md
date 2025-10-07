# WordVenture – Android Word Puzzle Game

## Table of Contents
- [WordVenture](#wordventure)
- [Overview](#overview)
- [Features](#features)
- [Gameplay](#gameplay)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [GitHub Actions & CI/CD](#github-actions--cicd)
- [Installation & Setup](#installation--setup)
- [API Integration](#api-integration)
- [Screens & UI](#screens--ui)
- [Future Improvements](#future-improvements)
- [Contributors](#contributors)

---

## WordVenture

**WordVenture** is a word-based puzzle game designed to challenge players’ problem-solving abilities. Players must guess randomised 5-letter words while exploring multiple game modes, personalisation features, and continuous gameplay designed to keep them engaged and entertained.  

The application’s design is modern and innovative, while still maintaining simplicity and accessibility.  
What sets **WordVenture** apart from the traditional Wordle experience is the introduction of **continuous play through progressive levels**, while still including the popular daily challenge to encourage community engagement and shared experiences.

---

## Overview

**WordVenture** transforms the familiar Wordle-style puzzle into a dynamic and evolving word adventure. The app aims to provide a fun yet mentally challenging experience by combining the simplicity of word guessing with progressive gameplay, statistics, and personalisation options.

Players can enjoy quick daily challenges, dive into continuous level-based play, or compete against each other in multiplayer modes — all while tracking their progress and customising their experience.

---

## Features

### Core Gameplay
- **Daily Word Challenge:** Solve a new 5-letter word puzzle each day, the same for all players.  
- **Continuous Play Mode:** Play unlimited puzzles in progressive levels.  
- **Multiplayer Mode:** Challenge friends locally to a match.

### Player Experience
- **Real-Time Feedback:** Colour-coded feedback for each guess (green, yellow, grey).  
- **Statistics:** Track win streaks, games played, and guess distribution.

### Account & Customisation
- **Secure Accounts:** Register and log in with Firebase authentication.  
- **Customisable UI:** Manage in-game audio.  
- **Profile Management:** Choose an avatar profile picture and update your username easily.

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
  - **Multiplayer:** Compete against your friends locally.

---

## Architecture

**WordVenture** follows a modular architecture designed for scalability, security, and performance.

- **Frontend:** Android app (Kotlin/Java) built with Android Studio  
- **Backend:** Node.js API providing daily words, validation, and statistics  
- **Database:** Firebase for user authentication and cloud data

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
  - Create a new Firebase project and add the google-services.json file to the /app directory.
4. **Run the App**
  - Connect an Android device or start an emulator.
  -	Click Run in Android Studio.

## API Integration
WordVenture connects to a custom Node.js backend API for dynamic content:
| Endpoint |	Method |	Description |
|----------|---------|--------------|
| /daily-word |	GET |	Fetches the daily 5-letter word |
| /guess |	POST |	Validates a user’s guess |

## Screens & UI
- Opening Page: displays WordVenture logo and options to register or login.
- Login / Register: secure authentication with email/password and SSO.
- Main Menu: access Daily, Continuous Play, and Multiplayer modes.
- Game Screen: interactive grid with live feedback and virtual keyboard.
- Profile & Settings: edit profile, manage audio, and contact help & support.

## Future Improvements
- Multi-language support
- Online multiplayer mode
- Friends list
- Biometric authentication

## Contributors
- Emma Mae Atkinson 
- Ayushkar Ramkissoon 
- Ethan Pillay

