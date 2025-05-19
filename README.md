# 🚗 Follow Me – Real-Time Trip Sharing App

**Follow Me** is a mobile application built for Android that allows users to **share and follow real-time trip progress** using a unique trip ID. It is designed to support both **Trip Leaders**, who share their location, and **Trip Followers**, who can track the trip in real time.

---

## 📱 Features

### 👤 Trip Leader
- Register and log in with a unique username/email
- Start a new trip with a unique trip ID (manual or auto-generated)
- Share real-time location updates during travel
- View a live polyline route on a map
- Pause or end trip location sharing at any time
- Display key trip information:
  - Current location (car icon)
  - Start time
  - Elapsed time
  - Distance traveled
- Share trip ID via SMS, email, etc.
- Supports background location updates

### 👀 Trip Follower
- Join an active trip using the shared trip ID
- Follow the Trip Leader’s route on an interactive map
- View:
  - Trip start time
  - Distance traveled
  - Elapsed trip time
- Auto-center map on the leader’s location or explore freely
- Resume tracking even after closing the app
- Receive notification when the trip ends

---

## 🌐 Backend API

Uses a RESTful API hosted at:
http://christopherhield-001-site4.htempurl.com

### Key Endpoints:
- `/api/UserAccounts/CreateUserAccount`
- `/api/UserAccounts/VerifyUserCredentials`
- `/api/Datapoints/AddTripPoint`
- `/api/Datapoints/GetTrip/{trip_id}`
- `/api/Datapoints/GetLastLocation/{trip_id}`
- `/api/Datapoints/TripExists/{trip_id}`

---

## 🛠️ Tech Stack

- **Platform:** Android (Java)
- **UI:** Android XML, ConstraintLayout
- **Location Services:** Fused Location Provider API
- **Map:** Google Maps SDK
- **Networking:** Android HTTP & JSON APIs
- **Background Tasks:** Services and BroadcastReceivers
- **Storage:** SharedPreferences
- **Icons & Assets:** Custom icons, animations, and fonts
- **API Communication:** JSON over HTTP (`usesCleartextTraffic="true"`)

---

## 🎨 UI Components

- Splash Screen & Main Menu
- Registration & Login Dialogs
- Trip Leader Map with Polyline, Distance & Timer Views
- Trip Follower Map with Tracking Controls
- Reusable Share and Pause/Play icons
- GPS & Network permissions with graceful fallback dialogs

---

## 🚧 Permissions Required

- `ACCESS_FINE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `POST_NOTIFICATIONS`

---
## screenshots
![image](https://github.com/user-attachments/assets/43989194-2d91-4cc8-9bf5-4c49d2e0bddd)
![image](https://github.com/user-attachments/assets/b38233a8-f278-4246-a83e-50fc8c101dfa)
![image](https://github.com/user-attachments/assets/daf9ed0a-889a-439e-a393-3c09bc70b233)
![image](https://github.com/user-attachments/assets/115a8a4f-239d-4633-9694-df0ac03f6b28)
![image](https://github.com/user-attachments/assets/bf58f039-0730-4fc9-af3b-bd02a182d7be)
![image](https://github.com/user-attachments/assets/7e9a274b-29a9-4bad-9dbd-c750b68dd57d)
![image](https://github.com/user-attachments/assets/5e26eb6c-721c-433c-80a3-09d1c9d7c238)
![image](https://github.com/user-attachments/assets/602f95b0-81a4-402a-9bd1-988e8d774b60)
![image](https://github.com/user-attachments/assets/e458b5e5-cf1c-4eed-a6b8-bc77d3c7dafb)
![image](https://github.com/user-attachments/assets/7bfbcab9-ef7c-4f0b-8a02-eb32342403bb)
![image](https://github.com/user-attachments/assets/81d8dcd9-8baa-4e37-a15e-45b8f30dca2b)
![image](https://github.com/user-attachments/assets/21e741a2-d711-4eaf-9370-6a5c32db1f8c)
![image](https://github.com/user-attachments/assets/f548c903-be34-425b-bf67-c364c8810429)
## 💡 How it Works (Simplified Flow)

### Trip Leader
1. Registers/logs in → Starts trip → Starts location updates
2. Shares trip ID → Sends updates to API
3. Polyline + car icon update in real time

### Trip Follower
1. Enters trip ID → Receives live location points from API
2. Map shows polyline and current location with car icon

---

## 📂 Resources Included

- 🎨 Icons: `car.png`, `pause.png`, `share_icon.png`, etc.
- 🗺️ Map images and custom layout XML
- 🔊 Sound: `notif_sound.mp3`
- 🆔 Font: `brandink_sans.otf`

---

## 🚀 Getting Started

1. Clone the repo
2. Open in Android Studio
3. Run on emulator or physical device (min SDK 29)
4. Grant permissions at runtime
5. Start or follow a trip!

---

## 👨‍💻 Author

**Anju Shaik**  
Graduate Student | Mobile Application Developer  
GitHub: [@shaikanju](https://github.com/shaikanju)

---

## 📜 License

This project is part of the **CSC 392/492: Mobile Applications Development for Android II** course at Jarvis College of Computing and Digital Media.

---


