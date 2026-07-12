package com.codealpha.chatbot;

import java.util.*;

public class IntentDatabase {
    private Map<String, List<String>> trainedPatterns = new HashMap<>();
    private Map<String, String> responses = new HashMap<>();

    public IntentDatabase() {
        seedTrainedData();
    }

    private void seedTrainedData() {
        // Intent 1: Greetings
        train("greeting", Arrays.asList(
            "hello", "hi there", "hey assistant", "greetings bot", "howdy", "yo", "good morning", "is anyone there"
        ));
        responses.put("greeting", "Hello! I am your AI assistant. How can I help you today?");

        // Intent 2: Identity / Scope
        train("identity", Arrays.asList(
            "who are you", "what is your name", "what do you do", "introduce yourself", "tell me about yourself", "your purpose"
        ));
        responses.put("identity", "I am a Java-based NLP Chatbot built for the CodeAlpha machine learning track, utilizing TF-IDF and Cosine Similarity mapping.");

        // Intent 3: Help / Capabilities
        train("help", Arrays.asList(
            "help me", "how to use this", "what are your features", "what can you do", "commands list", "show manual", "what are your limitations"
        ));
        responses.put("help", "I process sentences through mathematical vectors! Ask me who I am, check the weather, or test how I handle synonyms.");

        // Intent 4: Weather
        train("weather", Arrays.asList(
            "how is the weather", "is it raining outside", "forecast today", "weather update", "climate temperature"
        ));
        responses.put("weather", "Since I don't have active GPS integration yet, I'll forecast a 100% chance of clean code compiling today!");

        // Intent 5: Synonyms / Vocabulary
        train("synonyms", Arrays.asList(
            "give synonym", "similar meaning word", "words with same meaning", "tell synonym for word"
        ));
        responses.put("synonyms", "As an indexing vector space engine, I analyze similarity metrics rather than hosting a static thesaurus dictionary database.");

        // Fallback Default Response
        responses.put("unknown", "I'm sorry, my mathematical confidence score for that sentence is too low. Could you try rephrasing your question?");
    }

    private void train(String intent, List<String> patterns) {
        trainedPatterns.put(intent, patterns);
    }

    public Map<String, List<String>> getTrainedPatterns() {
        return trainedPatterns;
    }

    public String getResponse(String intent) {
        return responses.get(intent);
    }
}