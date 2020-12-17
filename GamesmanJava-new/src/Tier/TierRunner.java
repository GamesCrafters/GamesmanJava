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
        Connect4 game = new Connect4(w,h,win);
        int numTiers = w*h;
        SparkConf conf = new SparkConf().setAppName("appName").setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);
        List<Tuple2<Long, Piece[]>> data = new ArrayList<>();
        Tuple2<Long, Piece[]> temp = new Tuple2<>(0L, game.getStartingPositions());
        data.add(temp);
        JavaPairRDD<Long, Piece[]> distData = sc.parallelizePairs(data);

        String id = String.format("%sby%swin%s", w, h, win);
        Piece nextP = Piece.BLUE;
        for (int i = 1; i <= numTiers; i++) {
            distData.saveAsObjectFile(String.format("SPARK/%S/TIERS/%s/DOWN", id, i - 1));
            PairFlatMapFunction<Tuple2<Long, Piece[]>, Long, Piece[]> func = new DownwardThread(w, h, win, nextP, i);
            distData = distData.flatMapToPair(func).reduceByKey((v1, v2) -> v1);
            nextP = nextP.opposite();
        }
        nextP = nextP.opposite();
        // Bottoms are all primitives
        PairFunction<Tuple2<Long, Piece[]>, Long, Byte> primValue = new PrimValueThread(w, h, win, nextP, numTiers);
        JavaPairRDD<Long, Byte> pastPrimValues = distData.mapToPair(primValue);
        for (int i = numTiers - 1; i >= 0; i--) {
            Function<Piece[], List<Long>> findValueFunc = new ChildrenFuncThread(w, h, win, nextP, i);
            pastPrimValues.saveAsObjectFile(String.format("SPARK/%S/TIERS/%s/UP", id, i + 1));
            distData = JavaPairRDD.fromJavaRDD(sc.objectFile(String.format("SPARK/%S/TIERS/%s/DOWN", id, i)));
            JavaPairRDD<Long, List<Long>> childrenIDS = distData.mapValues(findValueFunc);
            sc.
            childrenIDS.join(pastPrimValues, ).
        }
        pastPrimValues.saveAsObjectFile(String.format("SPARK/%S/TIERS/%s/UP", id, 0));
    }




}
