#  Echo 🗣️

A decentralized, location-aware messaging application built with Kotlin Multiplatform using Aggregate Computing principles. Featuring GPS-based gossip algorithms for proximity communication.

[![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2FCollektive%2Fecho-app%2Fmain%2Fgradle%2Flibs.versions.toml&query=%24.versions.kotlin&label=Kotlin&color=blue)](https://kotlinlang.org)
[![Collektive](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2FCollektive%2Fecho-app%2Fmain%2Fgradle%2Flibs.versions.toml&query=%24.versions.collektive&label=Collektive&color=purple)](https://github.com/Collektive/collektive)
[![Compose Multiplatform](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2FCollektive%2Fecho-app%2Fmain%2Fgradle%2Flibs.versions.toml&query=%24.versions.composeMultiplatform&label=Compose%20Multiplatform&color=yellow)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

## 🌟 Features

- **📍 GPS-Based Messaging**: Send messages to devices within a specific geographic radius
- **🔄 Decentralized Gossip Algorithm**: Self-stabilizing message propagation without central control
- **📡 Real-Time Location Tracking**: Continuous GPS updates and neighbor discovery
- **⏱️ Configurable Parameters**: Set message lifetime and propagation distance 
- **🌐 Cross-Platform**: Runs on Android and iOS using Kotlin Multiplatform
- **📶 MQTT Communication**: Reliable message transport via MQTT protocol
- **🛠️ Built with Collektive**: Leverages the Collektive framework for aggregate computing

## 🏗️ Architecture

### System Design

Echo implements a **hybrid decentralized architecture**:

- **Application Layer (L7)**: Fully decentralized gossip algorithm
  - Self-stabilizing convergence
  - Distributed decision-making
  - No central coordinator
  
- **Transport Layer (L4)**: MQTT-based communication
  - Public broker for message routing
  - QoS guarantees for reliability
  - WebSocket support for mobile networks

### Key Components

```
┌─────────────────────────────────────────┐
│         Application Layer               │
│  (Decentralized Gossip Algorithm)       │
├─────────────────────────────────────────┤
│           MQTT Mailbox                  │
│      (Message Transport Layer)          │
├─────────────────────────────────────────┤
│        Location Services                │
│     (GPS Tracking & Distance)           │
├─────────────────────────────────────────┤
│      Collektive Framework               │
│   (Aggregate Computing Engine)          │
└─────────────────────────────────────────┘
```

## 🧮 Gossip Algorithm & Aggregate Computing

The app implements a **GPS-aware gradient gossip algorithm** built on **Aggregate Computing** principles using the **Collektive** framework.

### Decentralization
- **No central coordinator**: Each node operates autonomously
- **Distributed state**: Information spreads peer-to-peer through aggregate constructs
- **Local decisions**: Nodes use only neighbor information via Field abstractions
- **Global-to-local compilation**: Aggregate programs automatically map to decentralized execution

### Self-Stabilization
- **Convergence guarantee**: Reaches stable state through aggregate computation rounds
- **Fault tolerance**: Recovers from node failures via field-based resilience
- **Eventual consistency**: System converges despite asynchronous execution
- **Dynamic adaptation**: Adjusts to changing network conditions and node availability

### Spatial Awareness
- **Real distances**: Uses Haversine formula for GPS calculations between neighbors
- **Sender-controlled radius**: Message creator sets propagation distance (embedded in message)

