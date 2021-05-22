package com.hiveworkshop.pkb;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;

public class ColorSwapPreviewPanel extends JPanel {
	private final List<SwappedColor> swappedColors;

	public ColorSwapPreviewPanel(final List<SwappedColor> swappedColors) {
		setLayout(new GridLayout(swappedColors.size(), 1));
		this.swappedColors = swappedColors;
		for (final SwappedColor swappedColor : swappedColors) {
			add(new SingleColorSwapPanel(swappedColor));
		}
	}

	private static final class SingleColorSwapPanel extends JPanel {
		private final SwappedColor swappedColor;

		public SingleColorSwapPanel(final SwappedColor swappedColor) {
			this.swappedColor = swappedColor;
			add(Box.createRigidArea(new Dimension(48 * 3, 48)));
		}

		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			g.setColor(swappedColor.getPreviousValue());
			g.fillRect(0, 0, 48, 48);

			g.setColor(Color.BLACK);
			g.drawLine(86, 34, 96, 24);
			g.drawLine(48, 24, 96, 24);
			g.drawLine(86, 14, 96, 24);

			g.setColor(swappedColor.getNewValue());
			g.fillRect(48 * 2, 0, 48, 48);
		}
	}
}
