package com.codealpha.trading;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

/**
 * Class representing a single stock traded on the simulated exchange.
 * Manages pricing history for our real-time vector charting engine.
 */
class Stock {
    private String symbol;
    private String name;
    private double currentPrice;
    private double previousPrice;
    private double dailyHigh;
    private double dailyLow;
    private List<Double> priceHistory;
    private static final int MAX_HISTORY = 40;

    public Stock(String symbol, String name, double initialPrice) {
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = initialPrice;
        this.previousPrice = initialPrice;
        this.dailyHigh = initialPrice;
        this.dailyLow = initialPrice;
        this.priceHistory = new ArrayList<>();
        // Seed initial history to establish graph baselines
        for (int i = 0; i < MAX_HISTORY; i++) {
            priceHistory.add(initialPrice);
        }
    }

    // Mathematical Random Walk (Brownian Motion Simulation)
    public void fluctuate(Random rand) {
        this.previousPrice = this.currentPrice;
        // Standard distribution variance (-1.8% to +2.0% bias to simulate healthy markets)
        double percentChange = (rand.nextDouble() * 3.8) - 1.8;
        double delta = this.currentPrice * (percentChange / 100.0);
        this.currentPrice = Math.max(1.0, this.currentPrice + delta); // Protect against penny-stock drop to zero

        // Update day extremes
        if (this.currentPrice > this.dailyHigh) this.dailyHigh = this.currentPrice;
        if (this.currentPrice < this.dailyLow) this.dailyLow = this.currentPrice;

        // Maintain historical data array for graph rendering
        priceHistory.add(this.currentPrice);
        if (priceHistory.size() > MAX_HISTORY) {
            priceHistory.remove(0);
        }
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public double getChangePercent() {
        return ((currentPrice - previousPrice) / previousPrice) * 100.0;
    }
    public double getDailyHigh() { return dailyHigh; }
    public double getDailyLow() { return dailyLow; }
    public List<Double> getPriceHistory() { return priceHistory; }
}

/**
 * Encapsulates purchase records for average cost basis computations.
 */
class AssetHolding implements Serializable {
    private static final long serialVersionUID = 1L;
    private int shares;
    private double averageCost;

    public AssetHolding(int shares, double cost) {
        this.shares = shares;
        this.averageCost = cost;
    }

    public void addShares(int count, double price) {
        double totalCost = (this.shares * this.averageCost) + (count * price);
        this.shares += count;
        this.averageCost = totalCost / this.shares;
    }

    public boolean deductShares(int count) {
        if (count <= this.shares) {
            this.shares -= count;
            return true;
        }
        return false;
    }

    public int getShares() { return shares; }
    public double getAverageCost() { return averageCost; }
}

/**
 * Audit log entry model representing buy/sell operations.
 */
class Transaction {
    private String type; // BUY or SELL
    private String symbol;
    private int shares;
    private double price;
    private String timestamp;

    public Transaction(String type, String symbol, int shares, double price) {
        this.type = type;
        this.symbol = symbol;
        this.shares = shares;
        this.price = price;
        this.timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %d shares of %s @ $%.2f", timestamp, type, shares, symbol, price);
    }
}

/**
 * Keeps track of cash balance and holdings. Can persist to files.
 */
class Portfolio implements Serializable {
    private static final long serialVersionUID = 1L;
    private double cash;
    private Map<String, AssetHolding> holdings;

    public Portfolio(double initialCash) {
        this.cash = initialCash;
        this.holdings = new HashMap<>();
    }

    public double getCash() { return cash; }
    public Map<String, AssetHolding> getHoldings() { return holdings; }

    public void executeBuy(String symbol, int count, double price) {
        double cost = count * price;
        if (this.cash >= cost) {
            this.cash -= cost;
            holdings.putIfAbsent(symbol, new AssetHolding(0, 0.0));
            holdings.get(symbol).addShares(count, price);
        }
    }

    public boolean executeSell(String symbol, int count, double price) {
        AssetHolding holding = holdings.get(symbol);
        if (holding != null && holding.getShares() >= count) {
            this.cash += count * price;
            holding.deductShares(count);
            if (holding.getShares() == 0) {
                holdings.remove(symbol);
            }
            return true;
        }
        return false;
    }

    // Serialize Portfolio to disk (CodeAlpha Requirement 5 - File I/O option)
    public static void saveToFile(Portfolio portfolio, String filepath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath))) {
            oos.writeObject(portfolio);
        } catch (IOException e) {
            System.err.println("⚠️ Could not persist portfolio: " + e.getMessage());
        }
    }

    // Deserialize Portfolio from disk
    public static Portfolio loadFromFile(String filepath, double defaultCash) {
        File file = new File(filepath);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (Portfolio) ois.readObject();
            } catch (Exception e) {
                System.err.println("⚠️ Could not load portfolio, creating fresh instance.");
            }
        }
        return new Portfolio(defaultCash);
    }
}

/**
 * Custom line-graph rendering panel.
 * Dynamically scales and draws price history curves.
 */
class PriceChartPanel extends JPanel {
    private List<Double> priceHistory;
    private String symbol = "";
    
    // Modern Dashboard Theme Color Constants
    private final Color GRID_COLOR = new Color(51, 65, 85);
    private final Color LINE_COLOR = new Color(59, 130, 246);
    private final Color FILL_COLOR = new Color(59, 130, 246, 25);
    private final Color TEXT_COLOR = new Color(148, 163, 184);

    public PriceChartPanel() {
        setBackground(new Color(15, 23, 42)); // Match main app theme
    }

    public void updateChartData(String symbol, List<Double> history) {
        this.symbol = symbol;
        this.priceHistory = new ArrayList<>(history);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        // Turn on high-quality graphics antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 40;

        // Handle empty graph state
        if (priceHistory == null || priceHistory.isEmpty()) {
            g2.setColor(TEXT_COLOR);
            g2.drawString("Select a stock to view live vector historical analysis.", width / 3, height / 2);
            return;
        }

        // Draw structural Gridlines
        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 5; i++) {
            int y = padding + i * (height - 2 * padding) / 4;
            g2.drawLine(padding, y, width - padding, y);
        }

        // Compute local mathematical extremes (Min/Max Pricing)
        double maxPrice = Collections.max(priceHistory);
        double minPrice = Collections.min(priceHistory);
        double deltaPrice = maxPrice - minPrice;
        if (deltaPrice == 0) deltaPrice = 1.0; // Avoid mathematical division by zero exceptions

        // Map data arrays directly onto Java 2D coordinate space
        double chartWidth = width - 2 * padding;
        double chartHeight = height - 2 * padding;
        double xScale = chartWidth / (priceHistory.size() - 1);

        Path2D.Double linePath = new Path2D.Double();
        Path2D.Double areaPath = new Path2D.Double();

        for (int i = 0; i < priceHistory.size(); i++) {
            double price = priceHistory.get(i);
            double x = padding + (i * xScale);
            // Inverted Y-axis logic: math origin is top-left corner in graphics space
            double y = padding + chartHeight - (((price - minPrice) / deltaPrice) * chartHeight);

            if (i == 0) {
                linePath.moveTo(x, y);
                areaPath.moveTo(x, padding + chartHeight);
                areaPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                areaPath.lineTo(x, y);
            }

            if (i == priceHistory.size() - 1) {
                areaPath.lineTo(x, padding + chartHeight);
                areaPath.closePath();
            }
        }

        // Paint dynamic Area Fill (Gradient simulation)
        g2.setColor(FILL_COLOR);
        g2.fill(areaPath);

        // Draw Core Ticker Line
        g2.setColor(LINE_COLOR);
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(linePath);

        // Render Graph Annotations
        g2.setColor(TEXT_COLOR);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.drawString(symbol + " - Live Historical Performance Matrix", padding + 5, padding - 15);

        // Side Labels (High/Low Price Caps)
        DecimalFormat df = new DecimalFormat("$#,##0.00");
        g2.drawString(df.format(maxPrice), width - padding - 65, padding - 5);
        g2.drawString(df.format(minPrice), width - padding - 65, height - padding + 15);
    }
}

/**
 * Main application window.
 * Houses GUI layouts, simulated market tickers, and buy/sell actions.
 */
public class StockTradingPlatform extends JFrame {
    private static final long serialVersionUID = 1L;
    private final String DATA_FILE = "portfolio_data.dat";

    // Data Structures
    private Map<String, Stock> marketStocks;
    private Portfolio userPortfolio;
    private Stock selectedStock;
    private List<Transaction> transactionHistory;

    // UI Panel References
    private JTable marketTable;
    private DefaultTableModel marketTableModel;
    private PriceChartPanel chartPanel;
    private JTextArea statementArea;

    // Portfolio Dashboard Labels
    private JLabel cashValLabel;
    private JLabel equityValLabel;
    private JLabel netWorthValLabel;
    private JLabel profitLossValLabel;

    // Trading Panel Inputs
    private JLabel stockDetailTitle;
    private JLabel stockDetailPrice;
    private JLabel stockHoldingLabel;
    private JTextField qtyField;

    // Format utility
    private final DecimalFormat moneyFormat = new DecimalFormat("$#,##0.00");
    private final DecimalFormat percentFormat = new DecimalFormat("+0.00%;-0.00%");

    public StockTradingPlatform() {
        initializeApplicationState();
        buildDesktopUI();
        startMarketSimulationEngine();
    }

    private void initializeApplicationState() {
        // Load existing portfolio or construct fresh database state
        userPortfolio = Portfolio.loadFromFile(DATA_FILE, 50000.00); // Starter Cash
        transactionHistory = new ArrayList<>();

        // Populate market with diverse index sectors (Tech, Finance, Auto, Retail)
        marketStocks = new LinkedHashMap<>();
        marketStocks.put("AAPL", new Stock("AAPL", "Apple Inc.", 175.50));
        marketStocks.put("GOOGL", new Stock("GOOGL", "Alphabet Inc.", 142.20));
        marketStocks.put("TSLA", new Stock("TSLA", "Tesla Motors", 198.80));
        marketStocks.put("NVDA", new Stock("NVDA", "Nvidia Corp.", 875.00));
        marketStocks.put("AMZN", new Stock("AMZN", "Amazon.com Inc.", 178.40));
        marketStocks.put("MSFT", new Stock("MSFT", "Microsoft Corp.", 415.60));

        // Set baseline stock selection
        selectedStock = marketStocks.get("AAPL");
    }

    private void buildDesktopUI() {
        setTitle("CodeAlpha Enterprise Stock Trading Terminal");
        setSize(1280, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Core Layout Division (Left Pane: Market Monitor, Right Pane: Control Dashboard)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(480);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setBackground(new Color(15, 23, 42));

        // Create Left and Right views
        JPanel marketPanel = buildMarketWatchPanel();
        JPanel controlPanel = buildControlDashboardPanel();

        mainSplitPane.setLeftComponent(marketPanel);
        mainSplitPane.setRightComponent(controlPanel);

        add(mainSplitPane);

        // Save on window close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Portfolio.saveToFile(userPortfolio, DATA_FILE);
            }
        });
    }

    private JPanel buildMarketWatchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 41, 59));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(51, 65, 85)));

        // Frame header block
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(15, 23, 42));
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel title = new JLabel("📡 LIVE TICKER MARKET WATCH");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(new Color(248, 250, 252));
        headerPanel.add(title, BorderLayout.WEST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Set up the Live Market Grid
        String[] columns = {"Ticker", "Company", "Price", "24h Change"};
        marketTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        // Seed initial market model rows
        updateMarketTableRows();

        marketTable = new JTable(marketTableModel);
        marketTable.setRowHeight(45);
        marketTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        marketTable.setBackground(new Color(30, 41, 59));
        marketTable.setForeground(new Color(226, 232, 240));
        marketTable.getTableHeader().setBackground(new Color(15, 23, 42));
        marketTable.getTableHeader().setForeground(new Color(148, 163, 184));
        marketTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        marketTable.getTableHeader().setReorderingAllowed(false);
        marketTable.setShowVerticalLines(false);
        marketTable.setSelectionBackground(new Color(51, 65, 85));
        marketTable.setSelectionForeground(Color.WHITE);

        // Custom Cell Formatting (Up ticks green, down ticks red)
        marketTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
                c.setBackground(isSel ? new Color(51, 65, 85) : new Color(30, 41, 59));
                setBorder(noFocusBorder);

                if (col == 3) {
                    double change = Double.parseDouble(val.toString().replace("%", "").replace("+", ""));
                    if (change > 0) {
                        c.setForeground(new Color(16, 185, 129)); // Neon Emerald
                    } else if (change < 0) {
                        c.setForeground(new Color(239, 68, 68)); // Neon Ruby
                    } else {
                        c.setForeground(new Color(226, 232, 240));
                    }
                } else if (col == 0) {
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    c.setForeground(new Color(241, 245, 249));
                } else {
                    c.setForeground(new Color(148, 163, 184));
                }
                return c;
            }
        });

        // Add selection listener to change plotted chart stock dynamically
        marketTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = marketTable.getSelectedRow();
                if (row >= 0) {
                    String symbol = marketTable.getValueAt(row, 0).toString();
                    selectedStock = marketStocks.get(symbol);
                    updateDetailDisplay();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(marketTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(30, 41, 59));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildControlDashboardPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(15, 23, 42));

        // Upper Section: Real-Time Portfolio Dashboard
        JPanel topPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        topPanel.setBackground(new Color(15, 23, 42));
        topPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        cashValLabel = createValueCard(topPanel, "Available Cash", "$50,000.00", new Color(59, 130, 246));
        equityValLabel = createValueCard(topPanel, "Portfolio Valuation", "$0.00", new Color(139, 92, 246));
        netWorthValLabel = createValueCard(topPanel, "Net Worth", "$50,000.00", new Color(234, 179, 8));
        profitLossValLabel = createValueCard(topPanel, "Total Returns", "0.00%", new Color(16, 185, 129));

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center Split Section (Top: Chart panel, Bottom: Action Controls & logs)
        JPanel contentArea = new JPanel(new GridLayout(2, 1, 0, 10));
        contentArea.setBackground(new Color(15, 23, 42));
        contentArea.setBorder(new EmptyBorder(0, 15, 15, 15));

        // Custom Graphics Chart Panel
        chartPanel = new PriceChartPanel();
        chartPanel.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));
        contentArea.add(chartPanel);

        // Lower Layout Split (Left: Trade form, Right: Transaction log)
        JPanel lowerSplit = new JPanel(new GridLayout(1, 2, 15, 0));
        lowerSplit.setBackground(new Color(15, 23, 42));

        lowerSplit.add(buildTradePanel());
        lowerSplit.add(buildTransactionLogPanel());

        contentArea.add(lowerSplit);
        mainPanel.add(contentArea, BorderLayout.CENTER);

        // Preload initial dashboard metrics
        recalculatePortfolioMetrics();
        updateDetailDisplay();

        return mainPanel;
    }

    private JLabel createValueCard(JPanel container, String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 5));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85)),
            new EmptyBorder(12, 15, 12, 15)
        ));

        // Create colored visual accent tab
        JPanel tab = new JPanel();
        tab.setPreferredSize(new Dimension(4, 0));
        tab.setBackground(accentColor);
        card.add(tab, BorderLayout.WEST);

        JPanel textPane = new JPanel(new GridLayout(2, 1, 0, 3));
        textPane.setBackground(new Color(30, 41, 59));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLabel.setForeground(new Color(148, 163, 184));

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valLabel.setForeground(Color.WHITE);

        textPane.add(titleLabel);
        textPane.add(valLabel);
        card.add(textPane, BorderLayout.CENTER);

        container.add(card);
        return valLabel;
    }

    private JPanel buildTradePanel() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85)),
            new EmptyBorder(20, 25, 20, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Visual header
        stockDetailTitle = new JLabel("Apple Inc. (AAPL)");
        stockDetailTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        stockDetailTitle.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);
        card.add(stockDetailTitle, gbc);

        stockDetailPrice = new JLabel("$175.50");
        stockDetailPrice.setFont(new Font("Segoe UI", Font.BOLD, 22));
        stockDetailPrice.setForeground(new Color(59, 130, 246));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 15, 0);
        card.add(stockDetailPrice, gbc);

        // Security limits labels
        stockHoldingLabel = new JLabel("Currently Owned: 0 shares (Avg Cost: $0.00)");
        stockHoldingLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        stockHoldingLabel.setForeground(new Color(148, 163, 184));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 15, 0);
        card.add(stockHoldingLabel, gbc);

        // Quantity inputs
        JPanel qtyPanel = new JPanel(new BorderLayout(10, 0));
        qtyPanel.setBackground(new Color(30, 41, 59));
        JLabel qtyTitle = new JLabel("Trading Volume:");
        qtyTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        qtyTitle.setForeground(new Color(226, 232, 240));
        qtyPanel.add(qtyTitle, BorderLayout.WEST);

        qtyField = new JTextField("10");
        qtyField.setBackground(new Color(15, 23, 42));
        qtyField.setForeground(Color.WHITE);
        qtyField.setFont(new Font("Segoe UI", Font.BOLD, 14));
        qtyField.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));
        qtyField.setMargin(new Insets(5, 5, 5, 5));
        qtyPanel.add(qtyField, BorderLayout.CENTER);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 20, 0);
        card.add(qtyPanel, gbc);

        // Operation Execution Buttons
        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        actionPanel.setBackground(new Color(30, 41, 59));

        JButton buyBtn = new JButton("EXECUTE BUY ORDER");
        buyBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        buyBtn.setBackground(new Color(16, 185, 129)); // Green
        buyBtn.setForeground(Color.WHITE);
        buyBtn.setFocusPainted(false);
        buyBtn.addActionListener(e -> executeTransaction(true));

        JButton sellBtn = new JButton("EXECUTE SELL ORDER");
        sellBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sellBtn.setBackground(new Color(239, 68, 68)); // Red
        sellBtn.setForeground(Color.WHITE);
        sellBtn.setFocusPainted(false);
        sellBtn.addActionListener(e -> executeTransaction(false));

        actionPanel.add(buyBtn);
        actionPanel.add(sellBtn);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(actionPanel, gbc);

        return card;
    }

    private JPanel buildTransactionLogPanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85)),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel title = new JLabel("🧾 SYSTEM TRANSACTION LEDGER");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(new Color(148, 163, 184));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(title, BorderLayout.NORTH);

        statementArea = new JTextArea();
        statementArea.setBackground(new Color(15, 23, 42));
        statementArea.setForeground(new Color(16, 185, 129)); // Retro green log
        statementArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        statementArea.setEditable(false);
        statementArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane logScroll = new JScrollPane(statementArea);
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));
        card.add(logScroll, BorderLayout.CENTER);

        statementArea.append("[SYSTEM STARTED] Initializing audit log...\n");
        return card;
    }

    private void updateMarketTableRows() {
        marketTableModel.setRowCount(0);
        for (Stock s : marketStocks.values()) {
            marketTableModel.addRow(new Object[]{
                s.getSymbol(),
                s.getName(),
                moneyFormat.format(s.getCurrentPrice()),
                percentFormat.format(s.getChangePercent() / 100.0)
            });
        }
    }

    private void updateDetailDisplay() {
        if (selectedStock == null) return;

        stockDetailTitle.setText(selectedStock.getName() + " (" + selectedStock.getSymbol() + ")");
        stockDetailPrice.setText(moneyFormat.format(selectedStock.getCurrentPrice()));

        // Display holdings if they exist in user profile
        AssetHolding holding = userPortfolio.getHoldings().get(selectedStock.getSymbol());
        if (holding != null && holding.getShares() > 0) {
            stockHoldingLabel.setText(String.format("Currently Owned: %d shares (Avg Cost: %s)", 
                holding.getShares(), moneyFormat.format(holding.getAverageCost())));
        } else {
            stockHoldingLabel.setText("Currently Owned: 0 shares");
        }

        // Repaint Vector Canvas Graph
        chartPanel.updateChartData(selectedStock.getSymbol(), selectedStock.getPriceHistory());
    }

    private void executeTransaction(boolean isBuy) {
        if (selectedStock == null) return;

        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive integer quantity.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double price = selectedStock.getCurrentPrice();
        double totalCost = qty * price;

        if (isBuy) {
            // Check available capital limits
            if (userPortfolio.getCash() >= totalCost) {
                userPortfolio.executeBuy(selectedStock.getSymbol(), qty, price);
                logTransaction("BUY", selectedStock.getSymbol(), qty, price);
            } else {
                JOptionPane.showMessageDialog(this, "Purchase Denied: Insufficient cash balance.", "Transaction Error", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            // Check available assets limits
            boolean success = userPortfolio.executeSell(selectedStock.getSymbol(), qty, price);
            if (success) {
                logTransaction("SELL", selectedStock.getSymbol(), qty, price);
            } else {
                JOptionPane.showMessageDialog(this, "Sale Denied: You do not own enough shares of this stock.", "Transaction Error", JOptionPane.WARNING_MESSAGE);
            }
        }

        // Persist and synchronize UI states
        recalculatePortfolioMetrics();
        updateDetailDisplay();
    }

    private void logTransaction(String type, String symbol, int shares, double price) {
        Transaction tx = new Transaction(type, symbol, shares, price);
        transactionHistory.add(tx);
        statementArea.append("• " + tx.toString() + "\n");
        statementArea.setCaretPosition(statementArea.getDocument().getLength());
    }

    private void recalculatePortfolioMetrics() {
        double currentCash = userPortfolio.getCash();
        double equityValuation = 0.0;

        // Iterate holdings and calculate valuation dynamically
        for (Map.Entry<String, AssetHolding> entry : userPortfolio.getHoldings().entrySet()) {
            Stock currentStock = marketStocks.get(entry.getKey());
            if (currentStock != null) {
                equityValuation += entry.getValue().getShares() * currentStock.getCurrentPrice();
            }
        }

        double netWorth = currentCash + equityValuation;
        double initialBasisValue = 50000.00; // Starter cash index baseline
        double netProfitLoss = ((netWorth - initialBasisValue) / initialBasisValue) * 100.0;

        // Update main top dashboards cards
        cashValLabel.setText(moneyFormat.format(currentCash));
        equityValLabel.setText(moneyFormat.format(equityValuation));
        netWorthValLabel.setText(moneyFormat.format(netWorth));
        profitLossValLabel.setText(percentFormat.format(netProfitLoss / 100.0));

        // Color code total net returns
        if (netProfitLoss > 0) {
            profitLossValLabel.setForeground(new Color(16, 185, 129)); // Green
        } else if (netProfitLoss < 0) {
            profitLossValLabel.setForeground(new Color(239, 68, 68)); // Red
        } else {
            profitLossValLabel.setForeground(Color.WHITE);
        }
    }

    private void startMarketSimulationEngine() {
        Random rand = new Random();
        // Background thread ticking every 3.5 seconds to fluctuate prices
        Timer marketTimer = new Timer(3500, e -> {
            for (Stock s : marketStocks.values()) {
                s.fluctuate(rand);
            }
            // Sync calculations
            updateMarketTableRows();
            recalculatePortfolioMetrics();
            updateDetailDisplay();
        });
        marketTimer.start();
    }

    public static void main(String[] args) {
        // Apply System styling layouts
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new StockTradingPlatform().setVisible(true);
        });
    }
}