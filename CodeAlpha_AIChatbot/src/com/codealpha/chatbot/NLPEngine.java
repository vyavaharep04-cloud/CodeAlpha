package com.codealpha.chatbot;

import java.util.*;

public class NLPEngine {
    private IntentDatabase database;
    private List<String> intents;
    private List<Set<String>> tokenizedPatterns;
    private Set<String> globalVocabulary;
    private Map<String, Double> idfCache;

    // Define standard English stop-words to strip noise from vector calculations
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "what", "are", "your", "to", "do", "for", "the", "is", "a", "an", "and", 
        "or", "in", "of", "on", "it", "this", "that", "you", "i", "me", "how", "with", "about"
    ));

    public NLPEngine() {
        this.database = new IntentDatabase();
        this.intents = new ArrayList<>();
        this.tokenizedPatterns = new ArrayList<>();
        this.globalVocabulary = new HashSet<>();
        this.idfCache = new HashMap<>();
        buildVocabularyAndIndex();
        calculateIDFCache();
    }

    private int calculateLevenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];
        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1, 
                        dp[i][j - 1] + 1), 
                        dp[i - 1][j - 1] + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        return dp[x.length()][y.length()];
    }

    private String getClosestVocabularyWord(String word) {
        if (globalVocabulary.contains(word)) {
            return word;
        }
        String closest = word;
        int minDistance = 2; // Maximum edit threshold for corrections (e.g., 1 or 2 letter typos)
        for (String vocabWord : globalVocabulary) {
            int distance = calculateLevenshteinDistance(word, vocabWord);
            if (distance < minDistance) {
                minDistance = distance;
                closest = vocabWord;
            }
        }
        return closest;
    }

    // Advanced NLP: Text Normalization, Tokenization, and Stop-Word Filtering
    private List<String> tokenizeAndNormalize(String text) {
        String cleanText = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
        String[] rawTokens = cleanText.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            String trimmed = token.trim();
            // Crucial: Only keep the word if it is NOT a stop-word and not empty
            if (!trimmed.isEmpty() && !STOP_WORDS.contains(trimmed)) {
                // Apply our Dynamic Programming spell corrector before compiling vector tokens
                tokens.add(getClosestVocabularyWord(trimmed));
            }
        }
        return tokens;
    }

    // Indexer: Preparing dataset structures for vector calculations
    private void buildVocabularyAndIndex() {
        for (Map.Entry<String, List<String>> entry : database.getTrainedPatterns().entrySet()) {
            String intent = entry.getKey();
            for (String pattern : entry.getValue()) {
                List<String> tokens = tokenizeAndNormalize(pattern);
                if (!tokens.isEmpty()) {
                    intents.add(intent);
                    Set<String> uniqueTokens = new HashSet<>(tokens);
                    tokenizedPatterns.add(uniqueTokens);
                    globalVocabulary.addAll(tokens);
                }
            }
        }
    }

    // Mathematical IDF calculation across dataset
    private void calculateIDFCache() {
        int totalDocuments = tokenizedPatterns.size();
        for (String term : globalVocabulary) {
            int docsWithTerm = 0;
            for (Set<String> patternTokens : tokenizedPatterns) {
                if (patternTokens.contains(term)) {
                    docsWithTerm++;
                }
            }
            // IDF Formula: ln(1 + (Total Docs / Docs containing Term))
            double idf = Math.log(1.0 + ((double) totalDocuments / (1.0 + docsWithTerm)));
            idfCache.put(term, idf);
        }
    }

    // Constructing numerical TF-IDF vectors for Cosine Similarity mapping
    private Map<String, Double> createVector(List<String> tokens) {
        Map<String, Double> vector = new HashMap<>();
        Map<String, Integer> termFrequencies = new HashMap<>();

        // Term Frequency (TF) calculation
        for (String token : tokens) {
            termFrequencies.put(token, termFrequencies.getOrDefault(token, 0) + 1);
        }

        // TF-IDF Weight Assignment
        for (String term : termFrequencies.keySet()) {
            if (globalVocabulary.contains(term)) {
                double tf = (double) termFrequencies.get(term) / tokens.size();
                double idf = idfCache.getOrDefault(term, 0.0);
                vector.put(term, tf * idf);
            }
        }
        return vector;
    }

    // Cosine Similarity Algorithm
    private double calculateCosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        Set<String> allKeys = new HashSet<>(v1.keySet());
        allKeys.addAll(v2.keySet());

        for (String key : allKeys) {
            double val1 = v1.getOrDefault(key, 0.0);
            double val2 = v2.getOrDefault(key, 0.0);

            dotProduct += val1 * val2;
            magnitude1 += val1 * val1;
            magnitude2 += val2 * val2;
        }

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    public String matchIntent(String userInput) {
        List<String> inputTokens = tokenizeAndNormalize(userInput);
        if (inputTokens.isEmpty()) {
            return database.getResponse("unknown");
        }

        Map<String, Double> inputVector = createVector(inputTokens);
        String matchedIntent = "unknown";
        double highestSimilarity = 0.0;

        // Compare input vector against every indexed trained pattern vector
        for (int i = 0; i < tokenizedPatterns.size(); i++) {
            List<String> patternTokensList = new ArrayList<>(tokenizedPatterns.get(i));
            Map<String, Double> patternVector = createVector(patternTokensList);

            double similarity = calculateCosineSimilarity(inputVector, patternVector);
            if (similarity > highestSimilarity) {
                highestSimilarity = similarity;
                matchedIntent = intents.get(i);
            }
        }

        // Raised the dynamic confidence match threshold to 25% (0.25) to prevent loose assumptions
        if (highestSimilarity >= 0.25) {
            return database.getResponse(matchedIntent);
        }
        return database.getResponse("unknown");
    }
}
