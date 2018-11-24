package com.google.appengine.api.images;

import java.io.Serializable;

public interface Image extends Serializable {
	static Image DUMMY = new Image() {
		private static final long serialVersionUID = 1L;

		@Override
		public byte[] getImageData() {
			return new byte[0];
		}
	};
	
	public byte[] getImageData();
}
