/**
 * @author Rohan Jain
 * 
 */
import java.awt.image.BufferedImage

import javax.activation.MimetypesFileTypeMap
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream

import org.apache.tools.ant.taskdefs.Ant

includeTargets << grailsScript('_GrailsBootstrap')
includeTargets << grailsScript("_GrailsInit")

grailsHome = Ant.project.properties."environment.GRAILS_HOME"
basedir = Ant.project.properties."base.dir"
assestImagePath = basedir + "/" + "grails-app/assets/images/"
assestCompressedImagePath = basedir + "/" + "grails-app/assets/imagesCompressed/"

target('default': "Image Compression Script Running...") {
	depends compile, checkVersion, configureProxy, bootstrap

	grailsImageCompressDirs = grailsApp.config.grails.image.compress.dirs
	grailsImageCompressExcludedFiles = grailsApp.config.grails.image.compress.excluded.files
	
	println "${basedir}"
	
	File assestsImageDir = new File("${assestImagePath}");
	displayDirectoryContents(assestsImageDir)
}

def displayDirectoryContents(File assestsImageDir) {
	println "assestsImageDir"+assestsImageDir
	try {
		File[] files = assestsImageDir.listFiles();
		println "files"+files
		for (File file : files) {
			if (file.isDirectory()) {
				System.out.println("directory:" + file.getCanonicalPath());
				String assestCompressedImagePath = file.getCanonicalPath().toString().replace("${assestImagePath}","${assestCompressedImagePath}")
				println "assestCompressedImagePath"+assestCompressedImagePath
				File compressedImageFile = new File(assestCompressedImagePath);
				if(!compressedImageFile.exists()){
					println "Creating dir---->"+file.getCanonicalPath()
					compressedImageFile.mkdir()
				}
				displayDirectoryContents(file);
				
				
			} else {
				compressImage(file.getCanonicalPath())
			}
		}
	} catch (IOException e) {
		e.printStackTrace();
	}
}

def compressImage(String imageFilePath){
	
	File imageFile = new File(imageFilePath);
	println "imageFile"+imageFile
	try{
		println "FileType"+new MimetypesFileTypeMap().getContentType(imageFile)
		
	if(new MimetypesFileTypeMap().getContentType(imageFile) == "application/octet-stream"){
		println new MimetypesFileTypeMap().getContentType(imageFile)
		return
	}
	String assestCompressedImagePath = imageFile.getCanonicalPath().toString().replace("${assestImagePath}","${assestCompressedImagePath}")
	
	File compressedImageFile = new File(assestCompressedImagePath);
	println "compressedImageFile"+compressedImageFile
		if(!compressedImageFile.exists()){
			println "imageFile.isDirectory()"+imageFile.isDirectory()
			if(imageFile.isDirectory()){
				println "compressedImageFile.exists()"+compressedImageFile.exists()
				compressedImageFile.mkdirs()
			
			}
			
		}
	
	InputStream is = new FileInputStream(imageFile);
	OutputStream os = new FileOutputStream(compressedImageFile);
	
	
	float quality = 0.5f;

	// create a BufferedImage as the result of decoding the supplied InputStream
	BufferedImage image = ImageIO.read(is);

	// get all image writers for JPG format
	Iterator<ImageWriter> writers
	
	if(new MimetypesFileTypeMap().getContentType(imageFile) == "image/png"){
		println "Png Image"+imageFile.getName()
		writers = ImageIO.getImageWritersByFormatName("png");
		}
	else{
		writers = ImageIO.getImageWritersByFormatName("jpg");
	}
	
	if (!writers.hasNext())
		throw new IllegalStateException("No writers found");

	ImageWriter writer = (ImageWriter) writers.next();
	ImageOutputStream ios = ImageIO.createImageOutputStream(os);
	writer.setOutput(ios);

	ImageWriteParam param = writer.getDefaultWriteParam();

	if(new MimetypesFileTypeMap().getContentType(imageFile) != "image/png"){
		
	// compress to a given quality
	param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	param.setCompressionQuality(quality);
	}
	
	// appends a complete image stream containing a single image and
	//associated stream and image metadata and thumbnails to the output
	writer.write(null, new IIOImage(image, null, null), param);

	// close all streams
	is.close();
	os.close();
	ios.close();
	writer.dispose();
	}
	catch(Exception e){
		System.out.println("Skipped File"+imageFile.getName())
	}
}
setDefaultTarget('default')
