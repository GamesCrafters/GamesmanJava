package Tier;

import Games.Interfaces.Game;
import Games.Interfaces.KeyValueGame;
import Games.PieceGame.Functions.OutputFunc;
import Helpers.Tuple;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.api.java.StorageLevels.MEMORY_AND_DISK;


public class TierRunner {

    public static void main(String[] args) {
        Game game;
        try {
            Class<?> clazz = Class.forName(args[0]);
            if (Game.class.isAssignableFrom(clazz)) {
                Constructor<?> ctor = clazz.getConstructor(String[].class);
                String[] cArgs = new String[args.length - 1];
                System.arraycopy(args, 1, cArgs, 0, args.length - 1);
                game = (Game) ctor.newInstance((Object) cArgs);
            } else {
                throw new ClassNotFoundException("Class cannot be a game");
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.out.println("Cannot find class " + args[0]);
            return;
        }
        String id = String.format("SPARK_OUT/%s_%s", game.getName(), game.getVariant());
        if (!new File(id).mkdirs()) {
            System.out.println("Game already solved");
            return;
        }




        SparkConf conf = new SparkConf().setAppName(String.format("%s_Solver", game.getName())).setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);

        int numTiers = game.getNumTiers();

        if (game.positionIsLocation()) {
            System.out.println("Unimplemented");
        } else {
            KeyValueGame kvGame = (KeyValueGame) game;

            List<Tuple2<Long, Object>> data = new ArrayList<>();
            Object startingPosition = kvGame.getStartingPosition();
            Tuple2<Long, Object> temp = new Tuple2<>(kvGame.calculateLocation(startingPosition), startingPosition);
            data.add(temp);
            JavaPairRDD<Long, Object> distData = sc.parallelizePairs(data);

            List<JavaPairRDD<Long, Object>> savedData = new ArrayList<>(numTiers);


            kvGame.solveStarting();
            for (int i = 1; i <= numTiers; i++) {
                kvGame.solveStepDown();
                PairFlatMapFunction<Tuple2<Long, Object>, Long, Object> func = kvGame.getDownwardFunc();
                // Map to the next function and eliminate duplicates
                JavaPairRDD<Long, Object> next = distData.flatMapToPair(func).reduceByKey((v1, v2) -> v1).persist(MEMORY_AND_DISK);
                System.out.printf("Completed computing %d positions for tier: %d\n", next.count(), i);

                // Filter out the primitives from the previous tier
                distData = distData.filter(kvGame.getPrimitiveCheck()).persist(MEMORY_AND_DISK);
                System.out.printf("Completed computing %d primitive positions for tier: %d\n", distData.count(), i - 1);

                // Store the primitives
                savedData.add(distData);
                distData = next;

            }

            // Now map bottom tier primitives to a tuple of location and value
            PairFunction<Tuple2<Long, Object>, Long, Tuple<Byte, Object>> primValue = kvGame.getPrimitiveFunc();
            JavaPairRDD<Long, Tuple<Byte, Object>> pastPrimValues = distData.mapToPair(primValue);

            // NEED TO SAVE TO FILE HERE ???


            for (int i = numTiers - 1; i >= 0; i--) {
                System.out.printf("Starting writing tier: %d to disk\n", i + 1);
                //if (i % 4 == (numTiers - 3) % 4) { // Use if saving time/space
                pastPrimValues.foreachPartitionAsync(kvGame.getOutputFunction(id));  // Write last tier to file
                //}
                kvGame.solveStepUp();


                pastPrimValues = pastPrimValues.flatMapToPair(kvGame.getParentFunction());

                //parentFunc = new ParentFunc(w, h, win, nextP, true, game, i); // Parent of primitive

                //JavaPairRDD<Long, Piece[]> next = JavaPairRDD.fromJavaRDD(sc.textFile(String.format("SPARK/%S/TIERS/%s/DOWN", id, i)));
                primValue = kvGame.getPrimitiveFunc();
                JavaPairRDD<Long, Object> next = savedData.remove(savedData.size() - 1);
                pastPrimValues = pastPrimValues.union(next.mapToPair(primValue));

                next.unpersist();

                Function2<Tuple<Byte, Object>, Tuple<Byte, Object>, Tuple<Byte, Object>> combFunc = kvGame.getCombineFunc();
                pastPrimValues = pastPrimValues.reduceByKey(combFunc);


            }

            pastPrimValues.foreachPartition(new OutputFunc(id, 0)); // Write last thing
        }

    }




}
