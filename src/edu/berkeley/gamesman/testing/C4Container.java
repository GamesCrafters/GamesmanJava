package edu.berkeley.gamesman.testing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.TierGame;

/**
 * A testing class for playing against a perfect play database
 * 
 * @author dnspies
 */
public class C4Container extends JPanel implements ActionListener, KeyListener,
		WindowListener {
	private static final long serialVersionUID = -8073360248394686305L;

	ConnectFour game;

	private Configuration conf;

	JRadioButton xButton;

	JRadioButton oButton;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public C4Container(Configuration conf) {
		super();
		this.conf = conf;
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

	/**
	 * @param args
	 *            The job file
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		if (args.length != 1)
			throw new Error(
					"Please specify a database file as the only argument");
		Configuration conf;
		Database db;
		if (args[0].endsWith(".job")) {
			conf = new Configuration(args[0]);
			String uri = conf.getProperty("gamesman.db.uri");
			db = Database.openDatabase(uri, conf, true, false);
		} else {
			db = Database.openDatabase(args[0]);
			conf = db.conf;
		}
		int width = conf.getInteger("gamesman.game.width", 7);
		int height = conf.getInteger("gamesman.game.height", 6);
		Game<?> game = conf.getGame();
		Record r = game.newRecord();
		DatabaseHandle fdHandle = db.getHandle(true);
		TierGame g = (TierGame) game;
		g.longToRecord(g.hashToState(0), db.readRecord(fdHandle, 0), r);
		System.out.println(r);
		DisplayFour df = new DisplayFour(height, width);
		ConnectFour cf = new ConnectFour(conf, db, df);
		JFrame jf = new JFrame();
		Container c = jf.getContentPane();
		C4Container c4c = new C4Container(conf);
		c4c.setGame(cf);
		c.add(c4c);
		jf.addKeyListener(c4c);
		jf.setFocusable(true);
		jf.requestFocus();
		jf.setSize(width * 100, height * 100 + 125);
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jf.setVisible(true);
		jf.addWindowListener(c4c);
	}

	public void actionPerformed(ActionEvent ae) {
		game.compX = xButton.isSelected();
		game.compO = oButton.isSelected();
		game.startCompMove();
	}

	public void keyPressed(KeyEvent arg0) {
	}

	public void keyReleased(KeyEvent arg0) {
	}

	public void keyTyped(KeyEvent ke) {
		if (ke.getKeyChar() == 'r') {
			for (int c = 0; c < game.gameWidth; c++) {
				for (int r = 0; r < game.gameHeight; r++) {
					game.getDisplay().slots[r][c].removeMouseListener(game);
				}
			}
			setGame(new ConnectFour(conf, game.db, game.getDisplay(),
					xButton.isSelected(), oButton.isSelected()));
			repaint();
		}
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
		try {
			game.db.close();
		} catch (IOException e1) {
			throw new Error(e1);
		}
	}

	public void windowClosing(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}
}
