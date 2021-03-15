package Tier;

import Games.PieceGame.Connect4.Connect4;
import Games.PieceGame.RectanglePieceLocator;
import Helpers.Piece;
import org.apache.spark.api.java.function.Function;

import java.util.ArrayList;
import java.util.List;

public class ChildrenFuncThread implements Function<Piece[], List<Long>> {

    int w;
    int h;
    int win;
    Connect4 game;
    Piece nextP;
    RectanglePieceLocator locator;
    int tier;

    public ChildrenFuncThread(int w, int h, int win, Piece nextP, int tier) {
        locator = new RectanglePieceLocator(w, h);
        this.w = w;
        this.h = h;
        this.win = win;
        this.game = new Connect4(w,h,win);
        this.nextP = nextP;
        this.tier = tier;
    }

    @Override
    public List<Long> call(Piece[] pieces) {
        List<Long> ret = new ArrayList<>();
        List<Integer> moves = game.generateMoves(pieces);
        for (Integer move: moves) {
            ret.add(locator.calculateLocation(game.doMove(pieces, move, nextP), tier));
        }
        return ret;
    }
}
