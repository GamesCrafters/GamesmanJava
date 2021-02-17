package Tier;

import Games.Connect4;
import Helpers.Piece;

import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.*;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;


public class TierRunner {

    public static void main(String[] args) {
        int w = 3;
        int h = 3;
        int win = 3;
        int TIERLIMITNUM = 3;
        Connect4 game = new Connect4(w,h,win);
        int numTiers = w*h;
        SparkConf conf = new SparkConf().setAppName("Connect4Solver").setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);
        List<Tuple2<Long, Piece[]>> data = new ArrayList<>();
        Tuple2<Long, Piece[]> temp = new Tuple2<>(0L, game.getStartingPositions());
        data.add(temp);
        JavaPairRDD<Long, Piece[]> distData = sc.parallelizePairs(data);

        String id = String.format("%sby%swin%s", w, h, win);
        Piece nextP = Piece.BLUE;
        for (int i = 1; i <= numTiers; i++) {

            // Make the function that will generate next positions
            PairFlatMapFunction<Tuple2<Long, Piece[]>, Long, Piece[]> func = new DownwardThread(w, h, win, nextP, i);

            // Map to the next function and eliminate duplicates
            JavaPairRDD<Long, Piece[]> next = distData.flatMapToPair(func).reduceByKey((v1, v2) -> v1);

            distData = distData.filter(new PrimitiveFilter(w, h, win, nextP, game));
            distData.saveAsObjectFile(String.format("SPARK/%S/TIERS/%s/DOWN", id, i - 1)); // Save tier primitives

            distData = next;
            nextP = nextP.opposite();
        }
        // Undo the switch that was made
        nextP = nextP.opposite();

        // We only care about primitives, so filter those out
        //distData.filter(new PrimitiveFilter(w, h, win, nextP, game));// Unnecessary when bottom teir is all primitive

        // Now map bottom tier primitives to a tuple of location and value
        PairFunction<Tuple2<Long, Piece[]>, Long, Tuple<Byte, Piece[]>> primValue = new PrimValueThread(w, h, win, nextP, numTiers, game);
        JavaPairRDD<Long, Tuple<Byte, Piece[]>> pastPrimValues = distData.mapToPair(primValue);
        // NEED TO SAVE TO FILE HERE ???


        for (int i = numTiers - 1; i > 0; i--) {
            //if (i % 4 == (numTiers - 3) % 4) { // Use if saving time/space
                pastPrimValues.foreachPartitionAsync(new OutputFunc(i));  // Write last tier to file
            //}


            PairFlatMapFunction<Tuple2<Long, Tuple<Byte, Piece[]>>, Long, Tuple<Byte, Piece[]>> parentFunc = new ParentFunc(w, h, win, nextP, false, game, i); // Parent of non primitive

            pastPrimValues = pastPrimValues.flatMapToPair(parentFunc);


            parentFunc = new ParentFunc(w, h, win, nextP, true, game, i); // Parent of primitive
            JavaPairRDD<Long, Piece[]> next = JavaPairRDD.fromJavaRDD(sc.objectFile(String.format("SPARK/%S/TIERS/%s/DOWN", id, i)));
            pastPrimValues.union(next.mapToPair(primValue).flatMapToPair(parentFunc));

            Function2<Tuple<Byte, Piece[]>, Tuple<Byte, Piece[]>, Tuple<Byte, Piece[]>> combFunc = new ParentCombineFunc();
            pastPrimValues.reduceByKey(combFunc);
            nextP = nextP.opposite();
        }

        pastPrimValues.foreachPartitionAsync(new OutputFunc(0)); // Write last thing
    }




}
