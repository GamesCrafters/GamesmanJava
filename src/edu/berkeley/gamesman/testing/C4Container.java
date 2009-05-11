package edu.berkeley.gamesman.testing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.math.BigInteger;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.Util;

public class C4Container extends JPanel implements ActionListener, KeyListener {
    ConnectFour game;
    JRadioButton xButton;
    JRadioButton oButton;

    public C4Container() {
        super();
        setLayout(new BorderLayout());
        JPanel jp = new JPanel();
        jp.setLayout(new GridLayout(2, 3));
        add(jp, BorderLayout.SOUTH);
        add(new JLabel("Press 'r' to restart"), BorderLayout.NORTH);
        ButtonGroup bg = new ButtonGroup();
        jp.add(new JLabel("Red"));
        xButton = new JRadioButton("Computer");
        xButton.addKeyListener(this);
        JRadioButton jrb = new JRadioButton("Human");
        jrb.addKeyListener(this);
        bg.add(xButton);
        bg.add(jrb);
        jp.add(jrb);
        jp.add(xButton);
        jrb.setSelected(true);
        xButton.setSelected(false);
        jrb.addActionListener(this);
        xButton.addActionListener(this);
        bg = new ButtonGroup();
        jp.add(new JLabel("Black"));
        oButton = new JRadioButton("Computer");
        oButton.addKeyListener(this);
        jrb = new JRadioButton("Human");
        jrb.addKeyListener(this);
        bg.add(oButton);
        bg.add(jrb);
        jp.add(jrb);
        jp.add(oButton);
        jrb.setSelected(false);
        oButton.setSelected(true);
        jrb.addActionListener(this);
        oButton.addActionListener(this);
        jp.setFocusable(true);
        jp.addKeyListener(this);
    }

    private void setGame(ConnectFour cf) {
        add(cf.getDisplay(), BorderLayout.CENTER);
        game = cf;
    }

    public static void main(String[] args) throws InstantiationException,
            IllegalAccessException {
        if (args.length != 1) {
            Util.fatalError("Please specify a jobfile as the only argument");
        }
        Configuration conf;
        try {
            conf = new Configuration(args[0]);
        } catch (ClassNotFoundException e) {
            Util.fatalError("failed to load class", e);
            return;
        }
        Database fd;
        try {
            fd = conf.openDatabase();
            int width = conf.getInteger("gamesman.game.width", 7);
            int height = conf.getInteger("gamesman.game.height", 6);
            System.out.println(fd.getRecord(BigInteger.ZERO));
            DisplayFour df = new DisplayFour(height, width);
            ConnectFour cf = new ConnectFour(height, width, df, fd);
            JFrame jf = new JFrame();
            Container c = jf.getContentPane();
            C4Container c4c = new C4Container();
            c4c.setGame(cf);
            c.add(c4c);
            jf.addKeyListener(c4c);
            jf.setFocusable(true);
            jf.requestFocus();
            jf.setSize(375, 450);
            jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            jf.setVisible(true);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent ae) {
        game.compX = xButton.isSelected();
        game.compO = oButton.isSelected();
        game.startCompMove();
    }

    public void keyPressed(KeyEvent arg0) {}

    public void keyReleased(KeyEvent arg0) {}

    public void keyTyped(KeyEvent ke) {
        if (ke.getKeyChar() == 'r') {
            for (int c = 0; c < game.gameWidth; c++) {
                for (int r = 0; r < game.gameHeight; r++) {
                    game.getDisplay().slots[r][c].removeMouseListener(game);
                }
            }
            setGame(new ConnectFour(game.gameHeight, game.gameWidth, game
                    .getDisplay(), game.fd, xButton.isSelected(), oButton
                    .isSelected()));
            repaint();
        }
    }
}
