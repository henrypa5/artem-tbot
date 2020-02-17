First try to implement a Telegram Bot in Java.

# Build
Install maven and execute: 
```
mvn clean compile assembly:single
```
# Setup
- Invite bot to a chat.
- Execute /hiddengetbotchats
- Get the group Id for admins

Set environment variables:
```
CONNECT_ARTEM_BOT_TOKEN - generated Bot token
CONNECT_ARTEM_BOT_ADMIN_CHAT_ID - Admin chat ID
```
# Execute
```
java -jar target/tbot-0.0.1-SNAPSHOT-jar-with-dependencies.jar 
```
