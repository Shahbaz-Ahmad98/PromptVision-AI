
package com.ai.SpringAiDemo;

import com.ai.SpringAiDemo.user.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class ImageHistoryService {

    private final GeneratedImageRepository imageRepository;
    private final AppUserLookupRepository userRepository;
    private final Path storagePath;

    public ImageHistoryService(
            GeneratedImageRepository imageRepository,
            AppUserLookupRepository userRepository,
            @Value("${app.image.storage-path:./uploads/images}")
            String storagePath
    ) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.storagePath = Path.of(storagePath)
                .toAbsolutePath()
                .normalize();
    }

    private AppUser currentUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in"
            );
        }

        return userRepository.findByEmailIgnoreCase(
                authentication.getName()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authenticated user not found"
        ));
    }

    public void save(
            Authentication authentication,
            String prompt,
            byte[] imageBytes,
            String contentType
    ) {

        AppUser user = currentUser(authentication);

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException(
                    "Unsupported image type"
            );
        };

        String fileName = UUID.randomUUID() + extension;
        Path filePath = storagePath.resolve(fileName);

        try {
            Files.createDirectories(storagePath);

            Files.write(
                    filePath,
                    imageBytes,
                    StandardOpenOption.CREATE_NEW
            );

            GeneratedImage image = new GeneratedImage();

            image.setUserId(user.getId());
            image.setPrompt(prompt);
            image.setFileName(fileName);
            image.setContentType(contentType);

            try {
                imageRepository.save(image);
            } catch (RuntimeException ex) {
                Files.deleteIfExists(filePath);
                throw ex;
            }

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save generated image",
                    ex
            );
        }
    }

    public void saveDataUrls(
            Authentication authentication,
            String prompt,
            List<String> dataUrls
    ) {

        for (String dataUrl : dataUrls) {

            int comma = dataUrl.indexOf(',');

            if (!dataUrl.startsWith("data:image/")
                    || comma < 0) {
                throw new IllegalArgumentException(
                        "Invalid generated image data"
                );
            }

            String contentType = dataUrl.substring(
                    5,
                    dataUrl.indexOf(';')
            );

            byte[] imageBytes = Base64.getDecoder().decode(
                    dataUrl.substring(comma + 1)
            );

            save(authentication, prompt, imageBytes, contentType);
        }
    }

    public record ImageHistoryItem(
            Long id,
            String prompt,
            String contentType,
            java.time.LocalDateTime createdAt
    ) {}

    public List<ImageHistoryItem> getHistory(
            Authentication authentication
    ) {

        Long userId = currentUser(authentication).getId();

        return imageRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(image -> new ImageHistoryItem(
                        image.getId(),
                        image.getPrompt(),
                        image.getContentType(),
                        image.getCreatedAt()
                ))
                .toList();
    }

    private GeneratedImage findOwnedImage(
            Authentication authentication,
            Long imageId
    ) {

        Long userId = currentUser(authentication).getId();

        return imageRepository.findByIdAndUserId(
                imageId,
                userId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Image not found"
        ));
    }

    private Path safeFilePath(GeneratedImage image) {

        Path path = storagePath.resolve(
                image.getFileName()
        ).normalize();

        if (!path.getParent().equals(storagePath)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid stored image path"
            );
        }

        return path;
    }

    public GeneratedImage getImage(
            Authentication authentication,
            Long imageId
    ) {
        return findOwnedImage(authentication, imageId);
    }

    public byte[] getImageBytes(
            Authentication authentication,
            Long imageId
    ) {

        GeneratedImage image = findOwnedImage(
                authentication,
                imageId
        );

        try {
            return Files.readAllBytes(safeFilePath(image));
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Image file not found",
                    ex
            );
        }
    }

    public void deleteImage(
            Authentication authentication,
            Long imageId
    ) {

        GeneratedImage image = findOwnedImage(
                authentication,
                imageId
        );

        try {
            Files.deleteIfExists(safeFilePath(image));
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not delete image file",
                    ex
            );
        }

        imageRepository.delete(image);
    }
}
