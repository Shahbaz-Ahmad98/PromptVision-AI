
package com.ai.SpringAiDemo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/image-history")
public class ImageHistoryController {

    private final ImageHistoryService historyService;

    public ImageHistoryController(
            ImageHistoryService historyService
    ) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<ImageHistoryService.ImageHistoryItem> getHistory(
            Authentication authentication
    ) {
        return historyService.getHistory(authentication);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable Long id,
            Authentication authentication
    ) {

        GeneratedImage image = historyService.getImage(
                authentication,
                id
        );

        byte[] imageBytes = historyService.getImageBytes(
                authentication,
                id
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        image.getContentType()
                ))
                .body(imageBytes);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadImage(
            @PathVariable Long id,
            Authentication authentication
    ) {

        GeneratedImage image = historyService.getImage(
                authentication,
                id
        );

        byte[] imageBytes = historyService.getImageBytes(
                authentication,
                id
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        image.getContentType()
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + image.getFileName() + "\""
                )
                .body(imageBytes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            Authentication authentication
    ) {

        historyService.deleteImage(authentication, id);

        return ResponseEntity.noContent().build();
    }
}
