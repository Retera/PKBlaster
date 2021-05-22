package com.hiveworkshop.pkb;

import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class HorriblePKBlasterMain {

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final JFrame mainFrame = new JFrame("Horrible PKBlaster PKB Editor!!!!111 (v0.01b)");
				mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				final HorriblePKBlasterPanel panel = new HorriblePKBlasterPanel();
				mainFrame.setContentPane(panel);
				mainFrame.setJMenuBar(panel.createJMenuBar());
				mainFrame.setBounds(new Rectangle(800, 600));
				mainFrame.setLocationRelativeTo(null);
				mainFrame.setVisible(true);
			}
		});
	}

}
