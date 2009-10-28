for x in lib/*.jar; do
	export CLASSPATH="${CLASSPATH}:${x}"
done

CLASSPATH="${HADOOP_CONF_DIR}:$CLASSPATH"
CLASSPATH=${CLASSPATH}:$JAVA_HOME/lib/tools.jar

# for developers, add Hadoop classes to CLASSPATH
if [ -d "$HADOOP_HOME/build/classes" ]; then
  CLASSPATH=${CLASSPATH}:$HADOOP_HOME/build/classes
fi
if [ -d "$HADOOP_HOME/build/webapps" ]; then
  CLASSPATH=${CLASSPATH}:$HADOOP_HOME/build
fi
if [ -d "$HADOOP_HOME/build/test/classes" ]; then
  CLASSPATH=${CLASSPATH}:$HADOOP_HOME/build/test/classes
fi
if [ -d "$HADOOP_HOME/build/tools" ]; then
  CLASSPATH=${CLASSPATH}:$HADOOP_HOME/build/tools
fi

# so that filenames w/ spaces are handled correctly in loops below
IFS=

# for releases, add core hadoop jar & webapps to CLASSPATH
if [ -d "$HADOOP_HOME/webapps" ]; then
  CLASSPATH=${CLASSPATH}:$HADOOP_HOME
fi
for f in $HADOOP_HOME/hadoop-*-core.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

# add libs to CLASSPATH
for f in $HADOOP_HOME/lib/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

if [ -d "$HADOOP_HOME/build/ivy/lib/Hadoop/common" ]; then
for f in $HADOOP_HOME/build/ivy/lib/Hadoop/common/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done
fi

for f in $HADOOP_HOME/lib/jsp-2.1/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

for f in $HADOOP_HOME/hadoop-*-tools.jar; do
  TOOL_PATH=${TOOL_PATH}:$f;
done
for f in $HADOOP_HOME/build/hadoop-*-tools.jar; do
  TOOL_PATH=${TOOL_PATH}:$f;
done

# add user-specified CLASSPATH last
if [ "$HADOOP_CLASSPATH" != "" ]; then
  CLASSPATH=${CLASSPATH}:${HADOOP_CLASSPATH}
fi

# default log directory & file
if [ "$HADOOP_LOG_DIR" = "" ]; then
  HADOOP_LOG_DIR="$HADOOP_HOME/logs"
fi
if [ "$HADOOP_LOGFILE" = "" ]; then
  HADOOP_LOGFILE='hadoop.log'
fi

export CLASSPATH="${CLASSPATH}:${HADOOP_HOME}/hadoop-0.20.1-core.jar"
export CLASSPATH="bin:${CLASSPATH}"

