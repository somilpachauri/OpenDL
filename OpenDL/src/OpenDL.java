import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;

public class OpenDL extends JFrame implements DownloadListener {
    private JTextField urlField = new JTextField("");
    private JComboBox<Integer> threadCombo = new JComboBox<>(new Integer[]{4, 8, 16, 32});
    private JButton downloadBtn = new JButton("Start Download");
    private JButton pauseBtn = new JButton("Pause");
    private JButton historyBtn = new JButton("History");

    private JProgressBar overallProgress = new JProgressBar(0, 100);
    private JLabel statusLabel = new JLabel("Ready");
    private JLabel speedLabel = new JLabel("Speed: 0.00 MB/s");
    private JPanel threadsContainer = new JPanel();

    private JProgressBar[] threadBars;
    private long totalFileSize = 0;

    private DownloadTask currentTask = null;
    private String currentSavePath = "";

    public OpenDL() {
        setTitle("OpenDL - Download Manager");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        topPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
        topPanel.add(new JLabel("Download URL:"));
        topPanel.add(urlField);

        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        settingsPanel.add(new JLabel("Threads:"));
        settingsPanel.add(threadCombo);
        topPanel.add(settingsPanel);

        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        btnPanel.add(downloadBtn);

        pauseBtn.setEnabled(false);
        btnPanel.add(pauseBtn);
        btnPanel.add(historyBtn);
        topPanel.add(btnPanel);

        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        centerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        overallProgress.setStringPainted(true);
        centerPanel.add(overallProgress);
        JPanel statsPanel = new JPanel(new GridLayout(1, 2));
        statsPanel.add(statusLabel);
        statsPanel.add(speedLabel);
        centerPanel.add(statsPanel);
        add(centerPanel, BorderLayout.CENTER);

        threadsContainer.setLayout(new BoxLayout(threadsContainer, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(threadsContainer);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Thread Performance"));
        scrollPane.setPreferredSize(new Dimension(580, 200));
        add(scrollPane, BorderLayout.SOUTH);

        downloadBtn.addActionListener(e -> startDownload());
        historyBtn.addActionListener(e -> showHistoryDialog());

        pauseBtn.addActionListener(e -> {
            if (pauseBtn.getText().equals("Pause") && currentTask != null) {
                currentTask.pauseDownload();
                pauseBtn.setEnabled(false);
                pauseBtn.setText("Pausing...");
            } else if (pauseBtn.getText().equals("Resume")) {
                resumeDownload();
            }
        });

        LocalServer.startServer(this::receiveUrlFromBrowser);
    }

    public void receiveUrlFromBrowser(String url) {
        SwingUtilities.invokeLater(() -> {
            urlField.setText(url);
            setExtendedState(JFrame.NORMAL);
            setAlwaysOnTop(true);
            toFront();
            requestFocus();
            setAlwaysOnTop(false);
            if (downloadBtn.isEnabled()) {
                downloadBtn.doClick();
            }
        });
    }

    private void startDownload() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        downloadBtn.setEnabled(false);
        pauseBtn.setEnabled(true);
        pauseBtn.setText("Pause");
        threadsContainer.removeAll();
        overallProgress.setValue(0);

        String url = urlField.getText();
        currentSavePath = chooser.getSelectedFile().getAbsolutePath();
        int threads = (int) threadCombo.getSelectedItem();

        currentTask = new DownloadTask(url, currentSavePath, threads, this);
        new Thread(currentTask).start();
    }

    private void resumeDownload() {
        pauseBtn.setText("Pause");
        downloadBtn.setEnabled(false);
        String url = urlField.getText();
        int threads = (int) threadCombo.getSelectedItem();

        currentTask = new DownloadTask(url, currentSavePath, threads, this);
        new Thread(currentTask).start();
    }

    private void showHistoryDialog() {
        JDialog dialog = new JDialog(this, "Download History", true);
        dialog.setSize(700, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        String[] columns = {"Date", "File Name", "Save Path", "Size"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);

        Runnable loadData = () -> {
            model.setRowCount(0);
            for (String[] row : HistoryManager.getHistoryList()) {
                model.addRow(row);
            }
        };
        loadData.run();

        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton clearBtn = new JButton("Clear History");
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog, "Are you sure you want to clear all history?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                HistoryManager.clearHistory();
                loadData.run();
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(clearBtn);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }


    @Override
    public void onStart(long totalBytes, int actualThreads) {
        this.totalFileSize = totalBytes;
        SwingUtilities.invokeLater(() -> {
            threadBars = new JProgressBar[actualThreads];
            threadsContainer.removeAll();
            for (int i = 0; i < actualThreads; i++) {
                JPanel p = new JPanel(new BorderLayout(5, 5));
                p.setBorder(new EmptyBorder(2, 5, 2, 5));
                p.add(new JLabel("Th " + (i + 1)), BorderLayout.WEST);
                threadBars[i] = new JProgressBar(0, 100);
                threadBars[i].setStringPainted(true);
                p.add(threadBars[i], BorderLayout.CENTER);
                threadsContainer.add(p);
            }
            threadsContainer.revalidate();
            threadsContainer.repaint();

            if (totalBytes <= 0) {
                overallProgress.setIndeterminate(true);
            } else {
                overallProgress.setIndeterminate(false);
            }
        });
    }

    @Override
    public void onProgressUpdate(long downloadedBytes, double speedMBps) {
        SwingUtilities.invokeLater(() -> {
            double downloadedMB = downloadedBytes / (1024.0 * 1024.0);

            if (totalFileSize > 0) {
                int progress = (int) ((downloadedBytes * 100) / totalFileSize);
                overallProgress.setValue(progress);

                double totalMB = totalFileSize / (1024.0 * 1024.0);
                double remainingMB = totalMB - downloadedMB;
                statusLabel.setText(String.format("Downloaded: %.2f MB / %.2f MB (%.2f MB Remaining)", downloadedMB, totalMB, remainingMB));
            } else {
                statusLabel.setText(String.format("Downloaded: %.2f MB (Size Unknown)", downloadedMB));
            }
            speedLabel.setText(String.format("Speed: %.2f MB/s", speedMBps));
        });
    }

    @Override
    public void onThreadProgress(int threadId, int progressPercentage) {
        SwingUtilities.invokeLater(() -> {
            if (threadBars != null && threadId < threadBars.length) {
                if (progressPercentage < 0) {
                    threadBars[threadId].setIndeterminate(true);
                    threadBars[threadId].setStringPainted(false);
                } else {
                    threadBars[threadId].setIndeterminate(false);
                    threadBars[threadId].setStringPainted(true);
                    threadBars[threadId].setValue(progressPercentage);
                }
            }
        });
    }

    @Override
    public void onPaused() {
        SwingUtilities.invokeLater(() -> {
            pauseBtn.setText("Resume");
            pauseBtn.setEnabled(true);
            speedLabel.setText("Speed: Paused");
        });
    }

    @Override
    public void onComplete(String filePath) {
        SwingUtilities.invokeLater(() -> {
            overallProgress.setIndeterminate(false);
            overallProgress.setValue(100);
            for (JProgressBar bar : threadBars) if (bar != null) bar.setValue(100);
            statusLabel.setText("Download Complete!");
            speedLabel.setText("Speed: 0.00 MB/s");
            downloadBtn.setEnabled(true);
            pauseBtn.setEnabled(false);
            pauseBtn.setText("Pause");

            String fileName = new File(filePath).getName();
            String url = urlField.getText();
            HistoryManager.addDownload(fileName, url, filePath, totalFileSize);

            JOptionPane.showMessageDialog(this, "Saved to:\n" + filePath, "Success", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void onError(String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            overallProgress.setIndeterminate(false);
            statusLabel.setText("Error occurred.");
            downloadBtn.setEnabled(true);
            pauseBtn.setEnabled(false);
            pauseBtn.setText("Pause");
            JOptionPane.showMessageDialog(this, "Error: " + errorMessage, "Download Failed", JOptionPane.ERROR_MESSAGE);
        });
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new OpenDL().setVisible(true));
    }
}