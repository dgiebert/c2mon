##!/bin/sh

# TIM. CERN. All rights reserved.
#
# Wrapper script around the autogenerated scripts from CommonBuild for
# ~pcrops/dist/tim-daq/tim-daq-core. It is used to start and stop the
# individual DAQ message handlers on the specified DAQ machines.
#
# This version is adapted to TIM2 DAQ startup (simply different directories, HOME etc,
# and console output, otherwise identical).
#
# Also for TIM2, passes -transition argument in operational script.
#
# For more information, please go to the Wiki page:
# http://timweb/wiki/doku.php?id=documentation:howto#how_to_build_and_deploy_a_daq_module
# ------------------------------------------------------------------------------

# Start/stop/restart TIM2 DAQ process

#set home directory of script
SCRIPT=` readlink -f $(which $0)`
SCRIPTPATH=`dirname $SCRIPT`
DAQ_HOME=$SCRIPTPATH/..
SET_ENV_SCRIPT=$DAQ_HOME/bin/setenv.sh

#set the DAQ host machines if script is available
if [ -f $SET_ENV_SCRIPT ] ; then
  . $SET_ENV_SCRIPT
fi

#stop if variable not set
if [ -z $DAQ_PRIMARY_HOST ]; then
 echo "DAQ_PRIMARY_HOST is not set"
 exit 1
fi

#stop if variable not set
if [ -z $DAQ_SECONDARY_HOST ]; then
 echo "DAQ_SECONDARY_HOST is not set"
 exit 1
fi

#stop if variable not set
if [ -z $DAQ_OPC_HOST ]; then
 echo "DAQ_OPC_HOST is not set"
 exit 1
fi

# Used to check whether the DAQ that is going to be stopped is
# maybe still running on the obsolete DAQ host
#stop if variable not set
if [ -z $OBSOLETE_HOST ]; then
 echo "OBSOLETE_HOST is not set"
 exit 1
fi

DAQ_HOST=`hostname -s`

TIME=`date +"%F %T.%3N"`

#todo needs this to change according to deployment...
DAQ_SCRIPT=${DAQ_HOME}/bin/TIM2-DAQ-STARTUP.jvm

export DAQ_LOG_HOME
export DAQ_CONF_HOME=$DAQ_HOME/conf
export C2MON_PROPERTIES_FILE=$DAQ_CONF_HOME/c2mon.properties
DAQ_LOG_FILE=${DAQ_LOG_HOME}/daqprocess.log

# List of all OPC DAQ processes that are started on DAQ_SECONDARY_HOST
OPC_LIST=$DAQ_HOME/conf/opcdaqprocess.lst

# defines maximum number of processes that can run on one host
MAX_NUMBER_OF_PROCESSES_PER_HOST=60

# Creating directory structure, if it doesn't exit yet
if [ ! -d ${DAQ_HOME}/tmp/${DAQ_PRIMARY_HOST} ] ; then
  mkdir ${DAQ_HOME}/tmp/${DAQ_PRIMARY_HOST}
fi
if [ ! -d ${DAQ_HOME}/tmp/${DAQ_SECONDARY_HOST} ] ; then
  mkdir ${DAQ_HOME}/tmp/${DAQ_SECONDARY_HOST}
fi

# the name of the parameter determining that the DAQ start-up script
# should output only XML feedback messages

if [ "$1" == "-xml" ] ; then
  USE_XML_PROTOCOL=1
  export PROCESS_NAME=`echo $3 | tr 'a-z' 'A-Z'`
  PROCESS_COMMAND=$2
  export ADDITIONAL_PARAMS="$4 $5 $6 $7 $8 $9"
else
  USE_XML_PROTOCOL=0
  PROCESS_COMMAND=$1
  export PROCESS_NAME=`echo $2 | tr 'a-z' 'A-Z'`
  export ADDITIONAL_PARAMS="$3 $4 $5 $6 $7 $8 $9"
fi

PID_FILE_PRIMARY="${DAQ_HOME}/tmp/${DAQ_PRIMARY_HOST}/daqprocess_${PROCESS_NAME}.pid"
PID_FILE_SECONDARY="${DAQ_HOME}/tmp/${DAQ_SECONDARY_HOST}/daqprocess_${PROCESS_NAME}.pid"
PID_FILE_OPC="${DAQ_HOME}/tmp/${DAQ_OPC_HOST}/daqprocess_${PROCESS_NAME}.pid"
PID_FILE_OBSOLETE="${DAQ_HOME}/tmp/${OBSOLETE_HOST}/daqprocess_${PROCESS_NAME}.pid"

# Check which log4j configuration script should be used
if [ -f ${DAQ_CONF_HOME}/${PROCESS_NAME}_log4j.xml ] ; then
  export  LOG4J_CONF_FILE=${DAQ_CONF_HOME}/${PROCESS_NAME}_log4j.xml
else
  # set the default one
  export LOG4J_CONF_FILE=${DAQ_CONF_HOME}/log4j.xml
fi

# Make sure the JAVA_BIN variable points to the java bin directory on your machine
export JAVA_HOME=/usr/java/jdk
export PATH=$JAVA_HOME/bin:$JAVA_HOME/jre/bin:$PATH/


# Source function library.
if [ -f /etc/init.d/functions ] ; then
  . /etc/init.d/functions
elif [ -f /etc/rc.d/init.d/functions ] ; then
  . /etc/rc.d/init.d/functions
else
  exit 0
fi

RETVAL=0


# runs()
# The function will check whether a process with the
# specified PID is currently running. It will return
# 0 if the process is running, 1 if it isn't.
#
# Example: runs 23049
#
runs() {
  pid=${1##*/}
  tmp=`ps -p $pid -o pid=`
  if [ -z "$tmp" ] ; then
    return 1
  else
    return 0
  fi
}


# this procedure prepars the XML execution feedback message.
# It takes the following arguments :
#   1. the execution code
#   2. the execution status
#   3. value (not mandatory)
TIMDAQ_EchoXMLFeedback() {
  local EXEC_CODE=$1
  local EXEC_DESCR=$2
  local EXEC_VALUE=$3

  echo "<?xml version = \"1.0\"?>";
  echo "<execution-status>";
  echo "  <status-code>${EXEC_CODE}</status-code>";
  echo "  <status-description><![CDATA[${EXEC_DESCR}]]></status-description>";
  echo "</execution-status>";
  exit 0;
}

# This procedure starts the DAQ process only if it was not running yet.
TIMDAQ_start() {
  cd ${DAQ_HOME}

  # Check if the DAQ process is already running
  # If it is, don't start it again.
  if [ -f $PID_FILE ] ; then
    pid=`cat $PID_FILE | awk {'print $1'}`
    host=`cat $PID_FILE | awk {'print $2'}`
    runs $pid
    if [ $? -eq 1 ] ; then
      rm $PID_FILE
      really_start
    else

      if [ $USE_XML_PROTOCOL -eq 0 ] ; then
         echo_warning
         echo "DAQ Process ${PROCESS_NAME} seems to be running on host $host. Stop it first."
         echo -e "$TIME\t$PROCESS_NAME\t$DAQ_HOST\tSTART\tFAILED\t(running)" >> $DAQ_LOG_FILE
      else
         TIMDAQ_EchoXMLFeedback -1 "DAQ Process ${PROCESS_NAME} seems to be running. Stop it first."
      fi
    fi
  else
    really_start
  fi
}

# This producedure is in charge of:
# 1. Setting up some DAQ specific variables and deciding from where the DAQ process configuration should be read
# 2. Starting up the DAQ process by calling the common build autogenerated start-up script
# 3. Checking whether the process was started up successfully
really_start() {
  export DIM_DNS_NODE=dipns1,dipns2
  export DIM_DNS_PORT=2506
  export LASER_HOSTS=laser2,laser1
  export CMW_MOM_CONFIG=http://slwww/~alaser/console/laser-new/cmw-mom.properties


  if [ $PROCESS_NAME == 'P_SGATEWAY01' ] ; then
    export LD_ASSUME_KERNEL=2.4.1
  fi

  if [ -f ${DAQ_CONF_HOME}/local/${PROCESS_NAME}.xml ] ; then
    export ADDITIONAL_PARAMS="${ADDITIONAL_PARAMS} -c ${DAQ_CONF_HOME}/local/${PROCESS_NAME}.xml"
  fi

  export ADDITIONAL_PARAMS="${ADDITIONAL_PARAMS}"

  #Calls the script that was generated by the CommonBuild deployment procedure
   $DAQ_SCRIPT >${DAQ_LOG_HOME}/${PROCESS_NAME}.out.log 2>&1 &
  
  if [ $USE_XML_PROTOCOL -eq 0 ] ; then
    echo -n "Starting DAQ Process ${PROCESS_NAME} on host ${DAQ_HOST} ..."
  fi

  pid="$!"
  sleep 5
  runs $pid

  if [ $? -eq 1 ] ; then

    if [ $USE_XML_PROTOCOL -eq 0 ] ; then
      echo_failure
      echo "DAQ Process ${PROCESS_NAME} could not be started."
      echo
      echo -e "$TIME\t$PROCESS_NAME\t$DAQ_HOST\tSTART\tFAILED" >> $DAQ_LOG_FILE
    else
      TIMDAQ_EchoXMLFeedback -1 "DAQ Process ${PROCESS_NAME} could not be started."
    fi

  else
    echo "$pid $DAQ_HOST" > ${PID_FILE}
    if [ $USE_XML_PROTOCOL -eq 0 ] ; then
      echo_success
      echo
      echo -e "$TIME\t$PROCESS_NAME\t$DAQ_HOST\tSTART\tOK" >> $DAQ_LOG_FILE
    else
      TIMDAQ_EchoXMLFeedback 0 OK
    fi

  fi
}


# This procedure tries to gently kill the DAQ process. In case that the process cannot be killed in that way,
# it will force it
TIMDAQ_stop() {
 cd ${DAQ_HOME}

 if [ -f $PID_FILE ] ; then

   if [ $USE_XML_PROTOCOL -eq 0 ] ; then
     echo -n "Stopping DAQ Process ${PROCESS_NAME} on host ${DAQ_HOST} ..."
   fi

   pid=`cat $PID_FILE | awk {'print $1'}`
   kill $pid >/dev/null 2>&1
   runs $pid
   proc_runs=$?
   proc_wait=0
   while [ $proc_runs -eq 0 ] ; do
     if [ $USE_XML_PROTOCOL -eq 0 ] ; then
       echo -n .
     fi

     sleep 1
     if [ $proc_wait -lt 10 ] ; then
       let proc_wait=$proc_wait+1
       runs $pid
       proc_runs=$?
     else
       proc_runs=1
     fi
   done
   runs $pid

   if [ $? -eq 0 ] ; then

    if [ $USE_XML_PROTOCOL -eq 0 ] ; then
      echo_warning
      echo
      echo -n "Unable to stop DAQ Process ${PROCESS_NAME} gently... killing it..."
     fi

     kill -9 $pid
     sleep 1
     runs $pid

     if [ $? -eq 1 ] ; then
       rm -f $PID_FILE

       if [ $USE_XML_PROTOCOL -eq 0 ] ; then
         echo_success
         echo
         echo -e "$TIME\t$PROCESS_NAME\t$DAQ_HOST\tSTOP\tOK\t(kill -9)" >> $DAQ_LOG_FILE
       else
         TIMDAQ_EchoXMLFeedback 0 OK
       fi

       RETVAL=0
     else

       if [ $USE_XML_PROTOCOL -eq 0 ] ; then
         echo_failure
         echo
         echo -e "$TIME\t$PROCESS_NAME\t$DAQ_HOST\tSTOP\tFAILED" >> $DAQ_LOG_FILE
         echo "Unable to stop DAQ Process ${PROCESS_NAME}."
       else
         TIMDAQ_EchoXMLFeedback -1 "Unable to stop DAQ Process ${PROCESS_NAME}."
       fi

       RETVAL=1
     fi

   else

     if [ $USE_XML_PROTOCOL -eq 0 ] ; then
       echo_success
       echo
       echo -e "$TIME\t$PROCESS_NAME\t$DAQ_HOST\tSTOP\tOK" >> $DAQ_LOG_FILE
     else
       checkProcess=TIMDAQ_status4XML
       if [ $? -eq 0 ] ; then
     	 TIMDAQ_EchoXMLFeedback 0 OK
       else
         TIMDAQ_EchoXMLFeedback -1 "DAQ Process ${PROCESS_NAME} does not seem to be running"
       fi

     fi

     rm -f $PID_FILE
   fi

 else

   if [ $USE_XML_PROTOCOL -eq 1 ] ; then
     TIMDAQ_EchoXMLFeedback -1 "DAQ Process ${PROCESS_NAME} does not seem to be running"
   else
     echo "DAQ Process ${PROCESS_NAME} does not seem to be running"
   fi

 fi

}


# Check whether the DAQ process is running
# The function will return 0 if the DAQ is
# found to be running, 1 if it isn't.
# It will also display some messages indicating
# the status of the DAQ process in the output
# stream
# ----------------------------------------
TIMDAQ_status() {
  if [ -f $PID_FILE ]; then
    pid=`cat $PID_FILE`
    host=`cat $PID_FILE | awk {'print $2'}`
    runs $pid

    if [ $? -eq 0 ] ; then

      if [ $USE_XML_PROTOCOL -eq 0 ] ; then
   	    echo "RUNNING (host: $host  pid: $pid)"
      fi

      RETVAL=0
    else

      if [ $USE_XML_PROTOCOL -eq 0 ] ; then
        echo "DEAD (last time was running on host: $host) - cleaning PID file"
      fi

      rm -f $PID_FILE
      RETVAL=1
    fi
  else

    if [ $USE_XML_PROTOCOL -eq 0 ] ; then
      echo "STOPPED"
    fi

    RETVAL=1
  fi
  exit $RETVAL

}


  # Checks whether the DAQ Process is running.
  # The function will return 0 if the DAQ is found and
  # running and 1 if it isn't.
  TIMDAQ_status4XML() {

   #returns
   # 0 - RUNNING
   # 2 - STOPPED

  if [ -f $PID_FILE ]; then
    pid=`cat $PID_FILE`
    runs $pid
    if [ $? -eq 0 ] ; then
      RETVAL=0
    else
      rm -f $PID_FILE
      RETVAL=1
    fi
  else
    RETVAL=1
  fi

  return $RETVAL
}



# Restart: stop the DAQ, the start it again
TIMDAQ_restart() {
  TIMDAQ_stop
  sleep 1
  TIMDAQ_start
}

#
# Example: TIMDAQ_executeRemoteCmd cs-ccr-tim4 start
#
TIMDAQ_executeRemoteCmd() {
  instruction=${1}

  # if we're currently not on the machine we the process should be started on, ssh to that machine
  if [ `hostname -s` != $DAQ_HOST ] ; then
    CMD="$PROCESS_COMMAND $PROCESS_NAME $ADDITIONAL_PARAMS"
    if [ $USE_XML_PROTOCOL -eq 1 ] ; then
      CMD="-xml $CMD"
    fi
    #override the trap on the shell EXIT to prevent "logout" output
    ssh -2 $DAQ_HOST "trap '' EXIT; cd '$DAQ_HOME'/bin; $0 $CMD"
  else
    case "$instruction" in
     'start')
         TIMDAQ_start
     ;;

     'stop')
         TIMDAQ_stop
     ;;

     'status')
         TIMDAQ_status
     ;;

    esac
  fi
}

# Prints some instructions for the usage of this script.
# In particular it explains the supported arguments/options and how to use them.
TIMDAQ_printBasicUsageInfo() {
  if [ $USE_XML_PROTOCOL -eq 0 ] ; then
    echo "*****************************************************************************"
    echo " usage:                                                                      "
    echo " $0 [-xml] start|stop|status process_name [additional options]               "
    echo
    echo " if -xml parameter is specified, only the XML output will be served          "
    echo
    echo " The additional options are :                                                "
    echo "  -s filename       {saves received conf.xml in a file}                      "
    echo "  -c filename       {starts the DAQ using predefined conf. file,instead of   "
    echo "                     asking the app.server}                                  "
    echo "  -eqLoggers        {if enabled, the DAQ will create seperate file appenders "
    echo "                     for all equipment message handlers loggers}             "
    echo "  -eqAppendersOnly  {if placed in pair with -eqLoggers, the emh's output     "
    echo "                     will be redirected to specific emh's appender files     "
    echo "                     only. EMH's output will not affect the process logger}  "
    echo "  -testMode         {starts the DAQ in test mode. no JMS connections will be "
    echo "                     established}                                            "
    echo "  -noDeadband       {disables all dynamic deadband filtering; static         "
    echo "                     deadbands remain active}                                "
    echo "  -transition       {starts the DAQ in transiton configuration (TIM1 as main "
    echo "                     , TIM2 as second}                                       "
    echo " e.g: $0 start P_TEST01 -testMode -c /tmp/testconf.xml                       "
    echo "*****************************************************************************"
  else
    TIMDAQ_EchoXMLFeedback -1 "Improper entry arguments for the TIM DAQ start-up script detected. Check the configuration, please"
  fi
}

# ##########################################################################################################
# ################################           Main Routine:             #####################################
# ##########################################################################################################


  if [ -n "$PROCESS_NAME" ] ; then
    case "$PROCESS_COMMAND" in
     'start')

         # count number of pid files in the pid folder on primary daq host
         NUMBER_OF_PROCESSES=`ls ${DAQ_HOME}/tmp/${DAQ_PRIMARY_HOST} | wc -l`
         # The first condition checks wheather the process belongs to an OPC server. In that case
	 # it should ALWAYS be started on the OPC host.
         # The second condition checks the number if the maximum allowed number of processes running on the primary host
         # is reached or not. If so, switch to secondary host
	 if [ `grep -c ${PROCESS_NAME} ${OPC_LIST}` -ge 1 ] ; then
           # It's an OPC DAQ, so we start it on the dedicated OPC host
	   DAQ_HOST=$DAQ_OPC_HOST
	   PID_FILE=$PID_FILE_OPC
         # elif [ $MAX_NUMBER_OF_PROCESSES_PER_HOST -lt $NUMBER_OF_PROCESSES ] ; then
	 elif [ ${PROCESS_NAME} = "P_SGATEWAY01" ] ; then
           #the SmartGateway runs the old script as it will be discontinued
           export DAQ_HOME=$HOME/dist/daqprocess
           export DAQ_CONF_HOME=$DAQ_HOME/conf
           DAQ_SCRIPT=~/dist/daqprocess/build/bin/daqprocess
           export DAQ_LOG_HOME=/var/log/${USER}/daqprocess
           export LOG4J_CONF_FILE=${DAQ_CONF_HOME}/log4j.xml
           # set the default daqs host to secondary, because the two processes won't work on the primary one
           DAQ_HOST=$DAQ_SECONDARY_HOST
	   PID_FILE=$PID_FILE_SECONDARY
         else
           DAQ_HOST=$DAQ_PRIMARY_HOST
	   PID_FILE=$PID_FILE_PRIMARY
         fi
         TIMDAQ_executeRemoteCmd start
     ;;

     'stop')
         # set the default daq host and pid
         DAQ_HOST=$DAQ_PRIMARY_HOST
         PID_FILE=$PID_FILE_PRIMARY
         # find out, where the process is currently running
         # if on the secondary host, reset the default DAQ_HOST
         # and pid file variables
         if [ -f $PID_FILE_SECONDARY ] ; then
           DAQ_HOST=$DAQ_SECONDARY_HOST
           PID_FILE=$PID_FILE_SECONDARY
         elif [ -f $PID_FILE_OPC ] ; then
	   DAQ_HOST=$DAQ_OPC_HOST
	   PID_FILE=$PID_FILE_OPC
	 elif [ -f $PID_FILE_OBSOLETE ] ; then
	   DAQ_HOST=$OBSOLETE_HOST
	   PID_FILE=$PID_FILE_OBSOLETE
	 fi
         TIMDAQ_executeRemoteCmd stop
     ;;
     
     'status')
         # set the default daq host and pid
         DAQ_HOST=$DAQ_PRIMARY_HOST
         PID_FILE=$PID_FILE_PRIMARY
         # find out, where the process is currently running
         # if on the secondary host, reset the default DAQ_HOST
         # and pid file variables
         if [ -f $PID_FILE_SECONDARY ] ; then
           DAQ_HOST=$DAQ_SECONDARY_HOST
           PID_FILE=$PID_FILE_SECONDARY
         elif [ -f $PID_FILE_OPC ] ; then
           DAQ_HOST=$DAQ_OPC_HOST
           PID_FILE=$PID_FILE_OPC
	 elif [ -f $PID_FILE_OBSOLETE ] ; then
	   DAQ_HOST=$OBSOLETE_HOST
	   PID_FILE=$PID_FILE_OBSOLETE
         fi
         TIMDAQ_executeRemoteCmd status
     ;;

     *)
       TIMDAQ_printBasicUsageInfo
    esac
  else
    TIMDAQ_printBasicUsageInfo
  fi

