package Tier;

import Games.Connect4;
import Helpers.Piece;
import Helpers.Tuple;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class TierRunner {

    public static void main(String[] args) {
//        int w = Integer.parseInt(args[0]);
//        int h = Integer.parseInt(args[1]);
//        int win = Integer.parseInt(args[2]);

        int w = 1;
        int h = 1;
        int win = 1;

        Connect4 game = new Connect4(w,h,win);
        int numTiers = w*h;
        SparkConf conf = new SparkConf().setAppName("Connect4Solver").setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);

        List<Tuple2<Long, Piece[]>> data = new ArrayList<>();
        Tuple2<Long, Piece[]> temp = new Tuple2<>(0L, game.getStartingPositions());
        data.add(temp);
        JavaPairRDD<Long, Piece[]> distData = sc.parallelizePairs(data);

        List<JavaPairRDD<Long, Piece[]>> savedData = new ArrayList<>(numTiers);

        String id = String.format("SPARK_OUT/%sby%swin%s_%s", w, h, win, System.currentTimeMillis());
        new File(id).mkdirs();
        Piece nextP = Piece.BLUE;
        for (int i = 1; i <= numTiers; i++) {

            // Make the function that will generate next positions
            PairFlatMapFunction<Tuple2<Long, Piece[]>, Long, Piece[]> func = new DownwardThread(w, h, win, nextP, i);

            // Map to the next function and eliminate duplicates
            JavaPairRDD<Long, Piece[]> next = distData.flatMapToPair(func).reduceByKey((v1, v2) -> v1);

            distData = distData.filter(new PrimitiveFilter(w, h, win, nextP.opposite(), game));

            //distData.saveAsTextFile(String.format("SPARK/%S/TIERS/%s/DOWN", id, i - 1)); // Save tier primitives
            savedData.add(distData);
            distData = next;
            nextP = nextP.opposite();
        }

        // Undo the switch that was made
        Piece placed = nextP.opposite();


        // Now map bottom tier primitives to a tuple of location and value
        PairFunction<Tuple2<Long, Piece[]>, Long, Tuple<Byte, Piece[]>> primValue = new PrimValueThread(w, h, win, placed, numTiers, game);
        JavaPairRDD<Long, Tuple<Byte, Piece[]>> pastPrimValues = distData.mapToPair(primValue);

        // NEED TO SAVE TO FILE HERE ???


        for (int i = numTiers - 1; i >= 0; i--) {
            //if (i % 4 == (numTiers - 3) % 4) { // Use if saving time/space
                pastPrimValues.foreachPartition(new OutputFunc(id, i + 1));  // Write last tier to file
            //}


            PairFlatMapFunction<Tuple2<Long, Tuple<Byte, Piece[]>>, Long, Tuple<Byte, Piece[]>> parentFunc = new ParentFunc(w, h, win, placed, true, game, i); // Parent of non primitive

            pastPrimValues = pastPrimValues.flatMapToPair(parentFunc);

            placed = placed.opposite();

            //parentFunc = new ParentFunc(w, h, win, nextP, true, game, i); // Parent of primitive

            //JavaPairRDD<Long, Piece[]> next = JavaPairRDD.fromJavaRDD(sc.textFile(String.format("SPARK/%S/TIERS/%s/DOWN", id, i)));
            primValue = new PrimValueThread(w, h, win, placed, numTiers, game);
            JavaPairRDD<Long, Piece[]> next = savedData.remove(savedData.size() - 1);
            pastPrimValues = pastPrimValues.union(next.mapToPair(primValue));


            Function2<Tuple<Byte, Piece[]>, Tuple<Byte, Piece[]>, Tuple<Byte, Piece[]>> combFunc = new ParentCombineFunc();
            pastPrimValues = pastPrimValues.reduceByKey(combFunc);


        }

        pastPrimValues.foreachPartition(new OutputFunc(id, 0)); // Write last thing
    }




}
