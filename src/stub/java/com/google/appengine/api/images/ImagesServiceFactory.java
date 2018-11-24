package com.google.appengine.api.images;

public final class ImagesServiceFactory {
	public static ImagesService getImagesService() {
		return ImagesService.DUMMY;
	}
	
	public static Image makeImage(byte[] imageData) {
		return Image.DUMMY;
	}
	
	public static Transform makeVerticalFlip() {
		return CompositeTransform.DUMMY;
	}
	
	public static CompositeTransform makeCompositeTransform() {
		return CompositeTransform.DUMMY;
	}
}
