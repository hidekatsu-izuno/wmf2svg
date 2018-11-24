package com.google.appengine.api.images;

public interface ImagesService {
	static ImagesService DUMMY = new ImagesService() {
		@Override
		public Image applyTransform(Transform transform, Image image, OutputEncoding encoding) {
			return Image.DUMMY;
		}
	};
	
	public enum OutputEncoding {
		PNG,
		JPEG,
		WEBP
	}
	
	public Image applyTransform(Transform transform, Image image, ImagesService.OutputEncoding encoding);
}
