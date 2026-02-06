package com.laptophub.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(@Value("${cloudinary.url}") String cloudinaryUrl) {
        this.cloudinary = new Cloudinary(cloudinaryUrl);
    }

    public String uploadImage(MultipartFile file, String folder) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto"
                )
        );
        return (String) uploadResult.get("secure_url");
    }

    public void deleteImage(String imageUrl) throws IOException {
        String publicId = extractPublicIdFromUrl(imageUrl);
        if (publicId != null) {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            // Formato típico: https://res.cloudinary.com/{cloud}/image/upload/v{version}/{public_id}.{ext}
            int startIndex = url.lastIndexOf("/") + 1;
            int endIndex = url.lastIndexOf(".");
            if (startIndex > 0 && endIndex > startIndex) {
                return url.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
