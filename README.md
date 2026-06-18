# Chat Images <img width="31" height="31" alt="icon(1)" src="https://github.com/user-attachments/assets/e222bb92-5d3e-4f41-a9b4-c15edba99ab5" />

A Fabric mod for Minecraft 1.21.11 that lets you send and view images directly in the chat.

> **Note:** This mod is still rough around the edges — minor bugs may occur.

## Features

- **Send images in chat** — click the image button next to the chat input, select a file, and send it
- **Fullscreen viewer** — click any image in chat to open it fullscreen with zoom, pan, and drag support
- **Server-client detection** — the mod automatically detects whether the server supports Chat Images and disables the button if not

## Screenshots


<img width="278" height="102" alt="image" src="https://github.com/user-attachments/assets/5ae9432a-e365-4b6c-95d6-b416263ce3e2" />

*Image button in chat input*


<img width="680" height="448" alt="image" src="https://github.com/user-attachments/assets/34685c85-739d-406e-b9b3-a6e944cf83fe" />

*Image rendered inline in chat messages*


<img width="1920" height="1009" alt="image" src="https://github.com/user-attachments/assets/c1673b98-11a5-4b65-9eef-224d24d09997" />

<img width="1920" height="1009" alt="image" src="https://github.com/user-attachments/assets/534661d1-9ca6-4a66-a21f-eec385a1abad" />

*Fullscreen image viewer with zoom and drag*


<img width="1920" height="1080" alt="image" src="https://github.com/Stepan1411/Chat-images/blob/main/gif_support.gif" />

*Gif support*

## Usage

1. Install the mod on both the **server** and **client** (Fabric API required)
2. Join the server — the image button next to the chat input should be active
3. Click the button, select an image file, and send it
4. Click any image in chat to open it in the fullscreen viewer

## Commands

- `/chatimages test <player>` — test if a player has the mod installed
- `/chatimages reconnect <player>` — re-send the handshake to a player

## Compatibility

- Incompatible with **ChatAnimation**
- Requires **Fabric API**
