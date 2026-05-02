package com.digital.erc20.ui;

import com.digital.erc20.contract.ERC20Deployer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainWindow extends JFrame {

    // Network presets: name -> [rpcUrl, chainId, explorerUrl]
    private static final Map<String, String[]> NETWORKS = new LinkedHashMap<>();

    static {
        NETWORKS.put("Sepolia Testnet (Free ETH via faucet)", new String[]{
                "https://rpc.sepolia.org", "11155111", "https://sepolia.etherscan.io/address/"
        });
        NETWORKS.put("Holesky Testnet (Free ETH via faucet)", new String[]{
                "https://ethereum-holesky-rpc.publicnode.com", "17000", "https://holesky.etherscan.io/address/"
        });
        NETWORKS.put("Local Hardhat / Ganache (No fee needed)", new String[]{
                "http://127.0.0.1:8545", "1337", ""
        });
        NETWORKS.put("Custom Network", new String[]{
                "", "", ""
        });
    }

    private JComboBox<String> networkCombo;
    private JTextField rpcUrlField;
    private JTextField chainIdField;
    private JTextField explorerField;
    private JPasswordField privateKeyField;
    private JTextField tokenNameField;
    private JTextField tokenSymbolField;
    private JTextField initialSupplyField;
    private JButton deployButton;
    private JButton checkBalanceButton;
    private JTextArea logArea;
    private JTextField contractAddressField;
    private JTextField txHashField;
    private JLabel walletAddressLabel;

    public MainWindow() {
        setTitle("ERC20 Token Deployer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 780));
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(245, 247, 250));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildLog(), BorderLayout.SOUTH);

        add(root);
        pack();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("ERC20 Token Deployer", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(30, 80, 160));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCenter() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(buildNetworkPanel());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildWalletPanel());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildTokenPanel());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildActionPanel());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildResultPanel());

        return panel;
    }

    private JPanel buildNetworkPanel() {
        JPanel panel = titledPanel("Network");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        networkCombo = new JComboBox<>(NETWORKS.keySet().toArray(new String[0]));
        networkCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));

        rpcUrlField = styledTextField("https://rpc.sepolia.org");
        chainIdField = styledTextField("11155111");
        explorerField = styledTextField("https://sepolia.etherscan.io/address/");

        networkCombo.addActionListener(e -> onNetworkSelected());

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0.25;
        panel.add(label("Network:"), gc);
        gc.gridx = 1; gc.weightx = 0.75; gc.gridwidth = 3;
        panel.add(networkCombo, gc);

        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0.25;
        panel.add(label("RPC URL:"), gc);
        gc.gridx = 1; gc.weightx = 0.75; gc.gridwidth = 3;
        panel.add(rpcUrlField, gc);

        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0.25;
        panel.add(label("Chain ID:"), gc);
        gc.gridx = 1; gc.weightx = 0.35;
        panel.add(chainIdField, gc);
        gc.gridx = 2; gc.weightx = 0.15;
        panel.add(label("Explorer:"), gc);
        gc.gridx = 3; gc.weightx = 0.35;
        panel.add(explorerField, gc);

        onNetworkSelected();
        return panel;
    }

    private JPanel buildWalletPanel() {
        JPanel panel = titledPanel("Wallet");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        privateKeyField = new JPasswordField();
        privateKeyField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        privateKeyField.setPreferredSize(new Dimension(400, 28));

        walletAddressLabel = new JLabel("(enter private key to see address)");
        walletAddressLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        walletAddressLabel.setForeground(Color.GRAY);

        checkBalanceButton = styledButton("Check Balance", new Color(70, 130, 180));

        privateKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateAddress(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateAddress(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateAddress(); }
        });

        checkBalanceButton.addActionListener(e -> onCheckBalance());

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0.2;
        panel.add(label("Private Key:"), gc);
        gc.gridx = 1; gc.weightx = 0.8; gc.gridwidth = 2;
        panel.add(privateKeyField, gc);

        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 1;
        panel.add(label("Address:"), gc);
        gc.gridx = 1; gc.weightx = 0.6;
        panel.add(walletAddressLabel, gc);
        gc.gridx = 2; gc.weightx = 0.2;
        panel.add(checkBalanceButton, gc);

        return panel;
    }

    private JPanel buildTokenPanel() {
        JPanel panel = titledPanel("Token Configuration");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        tokenNameField = styledTextField("My Token");
        tokenSymbolField = styledTextField("MTK");
        initialSupplyField = styledTextField("1000000");

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0.2;
        panel.add(label("Token Name:"), gc);
        gc.gridx = 1; gc.weightx = 0.3;
        panel.add(tokenNameField, gc);
        gc.gridx = 2; gc.weightx = 0.2;
        panel.add(label("Symbol:"), gc);
        gc.gridx = 3; gc.weightx = 0.3;
        panel.add(tokenSymbolField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0.2;
        panel.add(label("Initial Supply:"), gc);
        gc.gridx = 1; gc.weightx = 0.3;
        panel.add(initialSupplyField, gc);
        gc.gridx = 2; gc.gridwidth = 2;
        panel.add(smallLabel("(tokens, 18 decimals applied automatically)"), gc);

        return panel;
    }

    private JPanel buildActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        panel.setOpaque(false);

        deployButton = styledButton("  Deploy ERC20 Contract  ", new Color(34, 139, 34));
        deployButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        deployButton.setPreferredSize(new Dimension(260, 42));
        deployButton.addActionListener(e -> onDeploy());

        panel.add(deployButton);
        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = titledPanel("Deployment Result");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        contractAddressField = styledTextField("");
        contractAddressField.setEditable(false);
        contractAddressField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        contractAddressField.setBackground(new Color(240, 255, 240));

        txHashField = styledTextField("");
        txHashField.setEditable(false);
        txHashField.setFont(new Font("Monospaced", Font.PLAIN, 12));

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0.2;
        panel.add(label("Contract Address:"), gc);
        gc.gridx = 1; gc.weightx = 0.8;
        panel.add(contractAddressField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0.2;
        panel.add(label("TX Hash:"), gc);
        gc.gridx = 1; gc.weightx = 0.8;
        panel.add(txHashField, gc);

        return panel;
    }

    private JPanel buildLog() {
        JPanel panel = titledPanel("Log");
        panel.setPreferredSize(new Dimension(0, 160));
        panel.setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(180, 255, 180));
        logArea.setMargin(new Insets(4, 6, 4, 6));

        JScrollPane scroll = new JScrollPane(logArea);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // --- Event handlers ---

    private void onNetworkSelected() {
        String selected = (String) networkCombo.getSelectedItem();
        String[] cfg = NETWORKS.get(selected);
        if (cfg != null && !cfg[0].isEmpty()) {
            rpcUrlField.setText(cfg[0]);
            chainIdField.setText(cfg[1]);
            explorerField.setText(cfg[2]);
            boolean custom = selected.startsWith("Custom");
            rpcUrlField.setEditable(custom || selected.startsWith("Local"));
            chainIdField.setEditable(custom);
        } else {
            rpcUrlField.setText("");
            chainIdField.setText("");
            explorerField.setText("");
            rpcUrlField.setEditable(true);
            chainIdField.setEditable(true);
        }
    }

    private void updateAddress() {
        String pk = new String(privateKeyField.getPassword()).trim();
        if (pk.length() >= 64) {
            try {
                String normalized = pk.startsWith("0x") ? pk.substring(2) : pk;
                org.web3j.crypto.Credentials creds = org.web3j.crypto.Credentials.create(normalized);
                walletAddressLabel.setText(creds.getAddress());
                walletAddressLabel.setForeground(new Color(0, 100, 0));
            } catch (Exception e) {
                walletAddressLabel.setText("Invalid private key");
                walletAddressLabel.setForeground(Color.RED);
            }
        } else {
            walletAddressLabel.setText("(enter private key to see address)");
            walletAddressLabel.setForeground(Color.GRAY);
        }
    }

    private void onCheckBalance() {
        String rpc = rpcUrlField.getText().trim();
        String pk = new String(privateKeyField.getPassword()).trim();
        if (rpc.isEmpty() || pk.isEmpty()) {
            log("Please fill in RPC URL and Private Key.");
            return;
        }
        checkBalanceButton.setEnabled(false);
        new Thread(() -> {
            try {
                long chainId = Long.parseLong(chainIdField.getText().trim());
                ERC20Deployer deployer = new ERC20Deployer(rpc, pk.startsWith("0x") ? pk.substring(2) : pk, chainId);
                BigInteger balWei = deployer.getBalance();
                double balEth = balWei.doubleValue() / 1e18;
                log(String.format("Balance: %.6f ETH (%s wei) — Address: %s", balEth, balWei, deployer.getAddress()));
                deployer.shutdown();
            } catch (Exception e) {
                log("Balance check failed: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> checkBalanceButton.setEnabled(true));
            }
        }).start();
    }

    private void onDeploy() {
        String rpc = rpcUrlField.getText().trim();
        String pk = new String(privateKeyField.getPassword()).trim();
        String name = tokenNameField.getText().trim();
        String symbol = tokenSymbolField.getText().trim();
        String supplyStr = initialSupplyField.getText().trim();

        if (rpc.isEmpty()) { showError("RPC URL is required."); return; }
        if (pk.isEmpty()) { showError("Private key is required."); return; }
        if (name.isEmpty()) { showError("Token name is required."); return; }
        if (symbol.isEmpty()) { showError("Token symbol is required."); return; }
        if (supplyStr.isEmpty()) { showError("Initial supply is required."); return; }

        BigInteger supply;
        long chainId;
        try {
            supply = new BigInteger(supplyStr);
            chainId = Long.parseLong(chainIdField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid supply or chain ID value.");
            return;
        }

        deployButton.setEnabled(false);
        contractAddressField.setText("");
        txHashField.setText("");
        log("--- Starting deployment ---");

        String normalizedPk = pk.startsWith("0x") ? pk.substring(2) : pk;
        long finalChainId = chainId;
        BigInteger finalSupply = supply;

        new Thread(() -> {
            ERC20Deployer deployer = null;
            try {
                deployer = new ERC20Deployer(rpc, normalizedPk, finalChainId);
                ERC20Deployer finalDeployer = deployer;

                ERC20Deployer.DeployResult result = finalDeployer.deploy(
                        name, symbol, finalSupply,
                        msg -> SwingUtilities.invokeLater(() -> log(msg))
                );

                String explorer = explorerField.getText().trim();
                String explorerLink = explorer.isEmpty() ? "" : "\nView on Explorer: " + explorer + result.contractAddress();

                SwingUtilities.invokeLater(() -> {
                    contractAddressField.setText(result.contractAddress());
                    txHashField.setText(result.txHash());
                    log("SUCCESS! Contract deployed at block " + result.blockNumber());
                    log("Contract: " + result.contractAddress());
                    log("TX Hash:  " + result.txHash());
                    if (!explorerLink.isEmpty()) log(explorerLink);
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    log("DEPLOYMENT FAILED: " + e.getMessage());
                    showError("Deployment failed:\n" + e.getMessage());
                });
            } finally {
                if (deployer != null) deployer.shutdown();
                SwingUtilities.invokeLater(() -> deployButton.setEnabled(true));
            }
        }).start();
    }

    // --- Helpers ---

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel titledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 220), 1),
                title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(30, 80, 160)
        ));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private JTextField styledTextField(String placeholder) {
        JTextField field = new JTextField(placeholder);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(200, 28));
        return field;
    }

    private JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        return lbl;
    }

    private JLabel smallLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lbl.setForeground(Color.GRAY);
        return lbl;
    }

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
