package edu.berkeley.gamesman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.berkeley.gamesman.util.Util;

public class GamesmanShell {

	public static void main(String args[]){
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while(true){
			System.out.print("gamesman> ");
			System.out.flush();
			String s = null;
			try {
				s = in.readLine();
			} catch (IOException e) {
				Util.fatalError("I/O error while reading from console",e);
			}
			if(s.trim().equals("")) return;
			try{
				Gamesman.main(s.split(" "));
			}catch(Util.FatalError e){
				System.out.println("Gamesman exited with a fatal error: ");
				System.out.println(e);
			}
		}
	}
	
}
