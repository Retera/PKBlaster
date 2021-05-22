package com.hiveworkshop.pkb;

import java.awt.Color;

public final class SwappedColor {
	private final Color previousValue;
	private final Color newValue;

	public SwappedColor(final Color previousValue, final Color newValue) {
		this.previousValue = previousValue;
		this.newValue = newValue;
	}

	public Color getPreviousValue() {
		return previousValue;
	}

	public Color getNewValue() {
		return newValue;
	}
}