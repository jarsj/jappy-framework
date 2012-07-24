package com.crispy;

import java.util.Set;
import java.util.TreeSet;

public class Size {
	private int width;
	private int height;

	public static Size create(int w, int h) {
		Size s = new Size();
		s.width = w;
		s.height = h;
		return s;
	}

	public static Set<Size> sizes(int... a) {
		Set<Size> ret = new TreeSet<Size>();
		for (int i = 0; i < a.length; i = i + 2) {
			Size s = new Size();
			s.width = a[i];
			s.height = a[i + 1];
			ret.add(s);
		}
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		Size other = (Size) obj;
		return (other.width == width) && (other.height == height);
	}
}
