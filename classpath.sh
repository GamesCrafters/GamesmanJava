for x in lib/*.jar; do
	export CLASSPATH="${CLASSPATH}:${x}"
done
export CLASSPATH="bin:${CLASSPATH}"

