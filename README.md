 CodeAlpha Java Development Internship

Welcome to my CodeAlpha internship repository! Instead of sticking to basic console scripts, I wanted to build complex desktop applications that demonstrate solid Java engineering, clean Object-Oriented Programming (OOP), and real-world math modeling.

📊 Task 2: Real-Time Stock Trading Terminal

This is a multi-threaded Desktop Financial Terminal built entirely from scratch using Java Swing. The app simulates a live-updating market environment where stock prices fluctuate dynamically every 3.5 seconds using a mathematical Random Walk model.

Instead of importing heavy external charting libraries, I wrote a custom vector graphics engine using Java's Graphics2D and Path2D to map price histories onto pixel coordinates dynamically. It features a robust double-entry ledger that tracks available cash, equity value, average cost-basis, and total returns with instant red/green visual feedback. Everything is kept fully persistent—when you buy or sell shares, the program logs audited transactions with timestamps and automatically serializes the entire user state into a local portfolio_data.dat file so you never lose your progress.

🤖 Task 3: TF-IDF Semantic NLP Chatbot

This is an intelligent desktop chatbot that uses vector space math instead of basic, fragile if-else keyword matching. It understands sentence intent even when users phrase things differently or make typos.

The core "AI" engine implements a custom TF-IDF (Term Frequency-Inverse Document Frequency) representation from scratch to measure word importance across the training vocabulary. When a user asks a question, the bot calculates the multidimensional angular alignment (Cosine Similarity) between the input vector and trained intent vectors to fetch the best match. I also built a dynamic programming Levenshtein Distance spelling corrector directly into the pipeline; if a user types a typo (like "wearher"), the corrector automatically maps it to the closest known vocabulary word ("weather") in real-time before running the similarity math.

🛠️ Setup & Execution

Requirements: Java SE (LTS 17 or higher) and any standard Java IDE (like Eclipse).

Execution: Clone the repository, import the source folders into your IDE workspace, and run either ChatbotGUI.java or StockTradingPlatform.java as a standard Java application!
