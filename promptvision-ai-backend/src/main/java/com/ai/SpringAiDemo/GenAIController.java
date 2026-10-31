
package com.ai.SpringAiDemo;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class GenAIController {

    private final ChatService chatService;
    private final ImageService imageService;
    private final RecipeService recipeService;
    private final ImageHistoryService imageHistoryService;

    public GenAIController(
            ChatService chatService,
            ImageService imageService,
            RecipeService recipeService,
            ImageHistoryService imageHistoryService
    ) {

        this.chatService = chatService;
        this.imageService = imageService;
        this.recipeService = recipeService;
        this.imageHistoryService = imageHistoryService;
    }

    // ==========================================
    // 1. NORMAL AI CHAT
    // ==========================================

    @GetMapping("/ask-ai")
    public String getResponse(
            @RequestParam String prompt
    ) {
        return chatService.getResponse(prompt);
    }

    // ==========================================
    // 2. AI CHAT WITH OPTIONS
    // ==========================================

    @GetMapping("/ask-ai-options")
    public String getResponseOptions(
            @RequestParam String prompt
    ) {
        return chatService.getResponseOptions(prompt);
    }

    // ==========================================
    // 3. GENERATE AND SAVE SINGLE IMAGE
    // ==========================================

    @GetMapping("/generate-image")
    public ResponseEntity<byte[]> generateImage(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "standard") String quality,
            @RequestParam(defaultValue = "1024") int width,
            @RequestParam(defaultValue = "1024") int height,
            @RequestParam(required = false) Integer steps,
            @RequestParam(defaultValue = "5.0") double cfgScale,
            @RequestParam(defaultValue = "0") int seed,
            Authentication authentication
    ) {

        byte[] image = imageService.generateImage(
                prompt,
                quality,
                width,
                height,
                steps,
                cfgScale,
                seed
        );

        String contentType = imageService.detectMimeType(image);

        imageHistoryService.save(
                authentication,
                prompt,
                image,
                contentType
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(image);
    }

    // ==========================================
    // 4. GENERATE AND SAVE MULTIPLE IMAGES
    // ==========================================

    @GetMapping("/generate-images")
    public List<String> generateImages(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "2") int n,
            @RequestParam(defaultValue = "standard") String quality,
            @RequestParam(defaultValue = "1024") int width,
            @RequestParam(defaultValue = "1024") int height,
            @RequestParam(required = false) Integer steps,
            @RequestParam(defaultValue = "5.0") double cfgScale,
            @RequestParam(defaultValue = "0") int seed,
            Authentication authentication
    ) {

        List<String> images = imageService.generateImages(
                prompt,
                n,
                quality,
                width,
                height,
                steps,
                cfgScale,
                seed
        );

        imageHistoryService.saveDataUrls(
                authentication,
                prompt,
                images
        );

        return images;
    }

    // ==========================================
    // 5. AI RECIPE GENERATOR
    // ==========================================

    @GetMapping("/generate-recipe")
    public String generateRecipe(
            @RequestParam String ingredients,
            @RequestParam(defaultValue = "Any") String cuisine,
            @RequestParam(defaultValue = "None")
            String dietaryRestrictions
    ) {

        if (ingredients == null || ingredients.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ingredients cannot be empty"
            );
        }

        return recipeService.createRecipe(
                ingredients,
                cuisine,
                dietaryRestrictions
        );
    }
}
