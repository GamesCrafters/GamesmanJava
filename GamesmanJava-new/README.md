Various solvers for connect4, including one using spark

VERSIONS --
Spark 2.4.7
Hadoop 2.7

Currently using the "Tier" package

Things for future:

Change byte value to 0-255 where maxing is always the best
Add interface for games so no direct calls
Optimize c4 hash/board state
    Add top down solver instead of bottom up

Bug fixing
Setup on savio machine

Add documentation

Justins group
C stuff




RUNNING\
To run, first build the project using mvn package if changes have been made, then copy 
the with-dependencies.jar.

Set the arguments inside SolverArgs.txt:\
gameClass={class path}\
example gameClass=Games.PieceGame.Connect4.Connect4

outPutFolder={path to folder} \
example outPutFolder=/global/scratch/{saviousername} if running on savio\
or outPutFolder=SPARK_OUT if running on local

Finally a list of classArg={arg} which are the arguments that your gameClass requires\
ex:\
classArg=4\
classArg=4\
classArg=4

To run the solver, call spark-submit --master {URL} path/to/jar path/to/SolverArgs

URL is local[n threads] if running locally, and is set automatically by the savio machines\
If running on windows use spark-submit.cmd instead


local example if running from the GamesmanJava-new dir:/
spark-submit --master local[8] GamesmanJava-new-1.0-jar-with-dependencies.jar SolverArgsLocal.txt

If you are getting an error about being unable to find spark submit, make sure that your SPARK/bin
directory is added to your PATH 

last edited by Jordan Bell 