package Tier;

import Games.Connect4;
import Helpers.Piece;

import Helpers.Primitive;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;


public class TierRunner {

    public static void main(String[] args) {
        int w = 4;
        int h = 5;
        int win = 2;
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
            // Save the downward positions
            distData.saveAsObjectFile(String.format("SPARK/%S/TIERS/%s/DOWN", id, i - 1));

            // Make the function that will generate next positions
            PairFlatMapFunction<Tuple2<Long, Piece[]>, Long, Piece[]> func = new DownwardThread(w, h, win, nextP, i);

            // Map to the next function and eliminate duplicates
            distData = distData.flatMapToPair(func).reduceByKey((v1, v2) -> v1);
            nextP = nextP.opposite();
        }
        // Undo the switch that was made
        nextP = nextP.opposite();

        // We only care about primitives, so filter those out
        // distData.filter(new PrimitiveFilter(w, h, win, nextP, game)); Unnecessary when bottom teir is all primitive

        // Now map bottom tier primitives to a tuple of location and value
        PairFunction<Tuple2<Long, Piece[]>, Long, Byte> primValue = new PrimValueThread(w, h, win, nextP, numTiers, game);
        JavaPairRDD<Long, Byte> pastPrimValues = distData.mapToPair(primValue);
        // NEED TO SAVE TO FILE HERE ???


        for (int i = numTiers - 1 - TIERLIMITNUM; i >= 0; i--) {
            Function<Piece[], List<Long>> findValueFunc = new ChildrenFuncThread(w, h, win, nextP, i);
            pastPrimValues.saveAsObjectFile(String.format("SPARK/%S/TIERS/%s/UP", id, i + 1));
            distData = JavaPairRDD.fromJavaRDD(sc.objectFile(String.format("SPARK/%S/TIERS/%s/DOWN", id, i)));
            JavaPairRDD<Long, List<Long>> childrenIDS = distData.mapValues(findValueFunc);

        }
        pastPrimValues.saveAsObjectFile(String.format("SPARK/%S/TIERS/%s/UP", id, 0));
    }




}
