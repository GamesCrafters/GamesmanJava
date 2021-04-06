package Tier;

import Games.Interfaces.Game;
import Games.Interfaces.HashlessGame;
import Games.Interfaces.KeyValueGame;
import Helpers.Tuple;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
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


    // Args should be: {Class Path of Game} args0 args1 ...
    // Example: Games.PieceGame.Connect4.Connect4 4 4 4
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

        if (game instanceof HashlessGame) {
            HashlessGame hashlessGame = (HashlessGame) game;


            List<Long> data = new ArrayList<>();
            data.add(hashlessGame.getStartingPosition());
            JavaRDD<Long> distData = sc.parallelize(data);

            List<JavaRDD<Long>> savedData = new ArrayList<>(numTiers);


            hashlessGame.solveStarting();
            for (int i = 1; i <= numTiers; i++) {
                hashlessGame.solveStepDown();
                FlatMapFunction<Long, Long> func = hashlessGame.getDownwardFunc();
                // Map to the next function and eliminate duplicates
                JavaRDD<Long> next = distData.flatMap(func).distinct().persist(MEMORY_AND_DISK);
                System.out.printf("Completed computing %d positions for tier: %d\n", next.count(), i);

                // Filter out the primitives from the previous tier
                distData = distData.filter(hashlessGame.getPrimitiveCheck()).persist(MEMORY_AND_DISK);
                System.out.printf("Completed computing %d primitive positions for tier: %d\n", distData.count(), i - 1);

                // Store the primitives
                savedData.add(distData);
                distData = next;

            }

            // Now map bottom tier primitives to a tuple of location and value
            PairFunction<Long, Long, Byte> primValue = hashlessGame.getPrimitiveFunc();
            JavaPairRDD<Long, Byte> pastPrimValues = distData.mapToPair(primValue);

            // NEED TO SAVE TO FILE HERE ???


            for (int i = numTiers - 1; i >= 0; i--) {
                System.out.printf("Starting writing tier: %d to disk\n", i + 1);
                //if (i % 4 == (numTiers - 3) % 4) { // Use if saving time/space
                pastPrimValues.foreachPartitionAsync(hashlessGame.getOutputFunction(id));  // Write last tier to file
                //}
                hashlessGame.solveStepUp();

                PairFlatMapFunction<Tuple2<Long, Byte>, Long, Byte> function = hashlessGame.getParentFunction();
                pastPrimValues = pastPrimValues.flatMapToPair(function);

                primValue = hashlessGame.getPrimitiveFunc();
                JavaRDD<Long> next = savedData.remove(savedData.size() - 1);
                pastPrimValues = pastPrimValues.union(next.mapToPair(primValue));

                next.unpersist();

                Function2<Byte, Byte, Byte> combFunc = hashlessGame.getCombineFunc();
                pastPrimValues = pastPrimValues.reduceByKey(combFunc);


            }

            pastPrimValues.foreachPartition(hashlessGame.getOutputFunction(id)); // Write last thing


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
            System.out.printf("Completed computing %d primitive positions for tier: %d\n", distData.count(), numTiers);
            // NEED TO SAVE TO FILE HERE ???


            for (int i = numTiers - 1; i >= 0; i--) {
                System.out.printf("Starting writing tier: %d to disk\n", i + 1);
                //if (i % 4 == (numTiers - 3) % 4) { // Use if saving time/space
                pastPrimValues.foreachPartitionAsync(kvGame.getOutputFunction(id));  // Write last tier to file
                //}
                kvGame.solveStepUp();


                pastPrimValues = pastPrimValues.flatMapToPair(kvGame.getParentFunction());


                primValue = kvGame.getPrimitiveFunc();
                JavaPairRDD<Long, Object> next = savedData.remove(savedData.size() - 1);
                pastPrimValues = pastPrimValues.union(next.mapToPair(primValue));

                next.unpersist();

                Function2<Tuple<Byte, Object>, Tuple<Byte, Object>, Tuple<Byte, Object>> combFunc = kvGame.getCombineFunc();
                pastPrimValues = pastPrimValues.reduceByKey(combFunc);


            }

            pastPrimValues.foreachPartition(kvGame.getOutputFunction(id)); // Write last thing
        }

    }




}
