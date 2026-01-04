package com.personal.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ImageService {
    
    @Value("${content.images.upload.path:/app/uploads/images}")
    private String uploadPath;
    
    @Value("${content.images.max.size:5242880}") // 5MB
    private long maxFileSize;
    
    private final ContentValidationService validationService;
    
    public ImageService(ContentValidationService validationService) {
        this.validationService = validationService;
    }
    
    public List<String> processAndStoreImages(List<MultipartFile> images) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        
        // Ensure upload directory exists
        createUploadDirectoryIfNotExists();
        
        for (MultipartFile image : images) {
            String imageUrl = processAndStoreImage(image);
            imageUrls.add(imageUrl);
        }
        
        return imageUrls;
    }
    
    public String processAndStoreImage(MultipartFile image) throws IOException {
        log.debug("Processing image: {}", image.getOriginalFilename());
        
        // Generate unique filename
        String filename = generateUniqueFilename(image.getOriginalFilename());
        
        // Read the original image
        BufferedImage originalImage = ImageIO.read(image.getInputStream());
        if (originalImage == null) {
            throw new IOException("Unable to read image file");
        }
        
        // Resize and optimize image for different contexts
        BufferedImage optimizedImage = resizeImage(originalImage, 800, 600);
        BufferedImage thumbnailImage = resizeImage(originalImage, 200, 200);
        
        // Save optimized image
        String optimizedPath = saveImage(optimizedImage, filename, "optimized");
        
        // Save thumbnail
        String thumbnailFilename = "thumb_" + filename;
        saveImage(thumbnailImage, thumbnailFilename, "thumbnails");
        
        log.info("Successfully processed and stored image: {}", filename);
        
        return "/images/optimized/" + filename;
    }
    
    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // Calculate new dimensions while maintaining aspect ratio
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth, newHeight;
        
        if (originalWidth > originalHeight) {
            newWidth = Math.min(maxWidth, originalWidth);
            newHeight = (int) (newWidth / aspectRatio);
            if (newHeight > maxHeight) {
                newHeight = maxHeight;
                newWidth = (int) (newHeight * aspectRatio);
            }
        } else {
            newHeight = Math.min(maxHeight, originalHeight);
            newWidth = (int) (newHeight * aspectRatio);
            if (newWidth > maxWidth) {
                newWidth = maxWidth;
                newHeight = (int) (newWidth / aspectRatio);
            }
        }
        
        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return resizedImage;
    }
    
    private String saveImage(BufferedImage image, String filename, String subdirectory) throws IOException {
        Path subdirPath = Paths.get(uploadPath, subdirectory);
        Files.createDirectories(subdirPath);
        
        File outputFile = subdirPath.resolve(filename).toFile();
        
        // Determine format from filename extension
        String format = getImageFormat(filename);
        
        boolean success = ImageIO.write(image, format, outputFile);
        if (!success) {
            throw new IOException("Failed to write image file: " + filename);
        }
        
        return outputFile.getAbsolutePath();
    }
    
    private String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + extension;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg"; // default extension
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private String getImageFormat(String filename) {
        String extension = getFileExtension(filename);
        switch (extension) {
            case "png":
                return "png";
            case "webp":
                return "webp";
            case "jpg":
            case "jpeg":
            default:
                return "jpg";
        }
    }
    
    private void createUploadDirectoryIfNotExists() throws IOException {
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
            log.info("Created upload directory: {}", uploadPath);
        }
        
        // Create subdirectories
        Files.createDirectories(Paths.get(uploadPath, "optimized"));
        Files.createDirectories(Paths.get(uploadPath, "thumbnails"));
    }
    
    public void deleteImage(String imageUrl) {
        try {
            // Extract filename from URL
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            
            // Delete optimized image
            Path optimizedPath = Paths.get(uploadPath, "optimized", filename);
            Files.deleteIfExists(optimizedPath);
            
            // Delete thumbnail
            String thumbnailFilename = "thumb_" + filename;
            Path thumbnailPath = Paths.get(uploadPath, "thumbnails", thumbnailFilename);
            Files.deleteIfExists(thumbnailPath);
            
            log.info("Deleted image files for: {}", filename);
            
        } catch (Exception e) {
            log.error("Error deleting image: {}", imageUrl, e);
        }
    }
    
    public boolean imageExists(String imageUrl) {
        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path imagePath = Paths.get(uploadPath, "optimized", filename);
            return Files.exists(imagePath);
        } catch (Exception e) {
            log.error("Error checking image existence: {}", imageUrl, e);
            return false;
        }
    }
}