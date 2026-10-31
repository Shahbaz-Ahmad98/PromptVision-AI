package com.ai.SpringAiDemo;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RecipeService {

    private final ChatModel chatModel;

    public RecipeService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String createRecipe(
            String ingredients,
            String cuisine,
            String dietaryRestrictions) {

        var template = """
                I want to create a recipe using the following ingredients: {ingredients}.
                The cuisine type I prefer is {cuisine}.
                Please consider the dietary restrictions: {dietaryRestrictions}.

                Please provide me with a detailed recipe including:
                1. Recipe title
                2. List of ingredients with quantities
                3. Preparation and cooking time
                4. Step-by-step cooking instructions
                5. Serving suggestions

                Use simple and clear English.
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);

        Map<String, Object> params = Map.of(
                "ingredients", ingredients,
                "cuisine", cuisine,
                "dietaryRestrictions", dietaryRestrictions
        );

        Prompt prompt = promptTemplate.create(params);

        return chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText();
    }
}
