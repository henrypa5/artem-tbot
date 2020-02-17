#!/bin/sh

# Setup variables
DESC="Artem Connection Telegram Bot"
EXEC=/usr/bin/jsvc
JAVA_HOME=/usr
CLASS_PATH=/home/opc/tbot/tbot-0.0.1-SNAPSHOT-jar-with-dependencies.jar
CLASS=org.telegram.artem.ConnectionBotService
USER=opc
PID=/tmp/tbot.pid
LOG_OUT=/tmp/tbot.out
LOG_ERR=/tmp/tbot.err
WORK_DIR=/home/opc/tbot

# Test bot
export CONNECT_ARTEM_BOT_TOKEN=***

# Prod bot
#export CONNECT_ARTEM_BOT_TOKEN=***

# Test group
export CONNECT_ARTEM_BOT_ADMIN_CHAT_ID=-***

# Prod group
# export CONNECT_ARTEM_BOT_ADMIN_CHAT_ID=-***

jsvc_exec()
{
   $EXEC -cwd "$WORK_DIR" -cp $CLASS_PATH -user $USER -outfile $LOG_OUT -errfile $LOG_ERR -pidfile $PID $1 $CLASS

   #$EXEC -Djavax.net.debug=all -cwd "$WORK_DIR" -cp $CLASS_PATH -user $USER -outfile $LOG_OUT -errfile $LOG_ERR -pidfile $PID $1 $CLASS
}

case "$1" in
   start)
   echo "Starting the $DESC..."         
      # Start the service
      jsvc_exec
   echo "The $DESC has started."
         ;;
   stop)
   echo "Stopping the $DESC..."
      # Stop the service
      jsvc_exec "-stop"
   echo "The $DESC has stopped."
         ;;
   restart)
      if [ -f "$PID" ]; then
      echo "Restarting the $DESC..."
         # Stop the service
         jsvc_exec "-stop"

         # Start the service
         jsvc_exec
      else
         echo "Service not running, will do nothing"
         exit 1
      fi
         ;;
   *)
         echo "usage: /etc/init.d/bot_service {start|stop|restart}" >&2
         exit 3
         ;;
esac
