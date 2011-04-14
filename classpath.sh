for x in lib/*.jar; do
	export CLASSPATH="${CLASSPATH}:${x}"
done

CLASSPATH=${CLASSPATH}:$JAVA_HOME/lib/tools.jar
export CLASSPATH="$PWD/bin:${CLASSPATH}"

