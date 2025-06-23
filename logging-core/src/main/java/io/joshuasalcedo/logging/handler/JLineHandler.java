package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.formatter.Formatter;
import io.joshuasalcedo.logging.formatter.SimpleFormatter;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintStream;

/**
 * JLine-based console handler with colored output and terminal detection
 */
public class JLineHandler implements Handler {
    private Formatter formatter = new SimpleFormatter();
    private LogLevel level = LogLevel.INFO;
    private Terminal terminal;
    private PrintStream out;
    private PrintStream err;
    private boolean colorEnabled = true;
    private boolean terminalDetected = false;
    
    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String DEBUG_COLOR = "\u001B[36m";    // Cyan
    private static final String INFO_COLOR = "\u001B[32m";     // Green
    private static final String WARN_COLOR = "\u001B[33m";     // Yellow
    private static final String ERROR_COLOR = "\u001B[31m";    // Red
    private static final String CRITICAL_COLOR = "\u001B[35m"; // Magenta
    private static final String BOLD = "\u001B[1m";
    
    public JLineHandler() {
        initialize();
    }
    
    public JLineHandler(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
        initialize();
    }
    
    private void initialize() {
        try {
            // Try to create JLine terminal
            this.terminal = TerminalBuilder.builder()
                .system(true)
                .build();
            
            this.out = new PrintStream(terminal.output());
            this.err = new PrintStream(terminal.output());
            this.terminalDetected = true;
            
            // Disable colors if terminal doesn't support them or color is disabled
            if (!colorEnabled || !terminal.getType().equals("ansi")) {
                this.colorEnabled = false;
            }
            
        } catch (IOException | UnsupportedOperationException e) {
            // Fallback to standard System.out/err if JLine fails
            this.terminal = null;
            this.out = System.out;
            this.err = System.err;
            this.terminalDetected = false;
            this.colorEnabled = false;
        }
    }
    
    @Override
    public synchronized void publish(Log record) {
        if (record.getLevel().getValue() < level.getValue()) {
            return;
        }
        
        try {
            String formattedMessage = formatter.format(record);
            String coloredMessage = colorEnabled ? colorizeMessage(record.getLevel(), formattedMessage) : formattedMessage;
            
            // Use err stream for ERROR and CRITICAL, out stream for others
            PrintStream targetStream = (record.getLevel() == LogLevel.ERROR || record.getLevel() == LogLevel.CRITICAL) ? err : out;
            
            targetStream.println(coloredMessage);
            targetStream.flush();
            
        } catch (Exception e) {
            // Fallback to System.err if there's any issue
            System.err.println("JLineHandler error: " + e.getMessage());
            System.err.println(formatter.format(record));
        }
    }
    
    private String colorizeMessage(LogLevel level, String message) {
        if (!colorEnabled) {
            return message;
        }
        
        String color = getColorForLevel(level);
        return color + message + RESET;
    }
    
    private String getColorForLevel(LogLevel level) {
        return switch (level) {
            case DEBUG -> DEBUG_COLOR;
            case INFO -> INFO_COLOR;
            case WARN -> BOLD + WARN_COLOR;
            case ERROR -> BOLD + ERROR_COLOR;
            case CRITICAL -> BOLD + CRITICAL_COLOR;
            default -> INFO_COLOR; // Fallback for any unexpected values
        };
    }
    
    @Override
    public void setFormatter(Formatter formatter) {
        this.formatter = formatter != null ? formatter : new SimpleFormatter();
    }
    
    @Override
    public void setLevel(LogLevel level) {
        this.level = level != null ? level : LogLevel.INFO;
    }
    
    @Override
    public LogLevel getLevel() {
        return level;
    }
    
    @Override
    public void close() {
        try {
            if (out != null && out != System.out) {
                out.flush();
            }
            if (err != null && err != System.err) {
                err.flush();
            }
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing JLineHandler: " + e.getMessage());
        }
    }
    
    // Getter methods for configuration and status
    public boolean isColorEnabled() {
        return colorEnabled;
    }
    
    public void setColorEnabled(boolean colorEnabled) {
        this.colorEnabled = colorEnabled && terminalDetected;
    }
    
    public boolean isTerminalDetected() {
        return terminalDetected;
    }
    
    public String getTerminalType() {
        return terminal != null ? terminal.getType() : "fallback";
    }
    
    public boolean isTerminalDumb() {
        return terminal != null && "dumb".equals(terminal.getType());
    }
    
    /**
     * Test method to print a color test pattern
     */
    public void printColorTest() {
        if (!colorEnabled) {
            out.println("Color output is disabled");
            return;
        }
        
        out.println("JLine Color Test:");
        for (LogLevel level : LogLevel.values()) {
            String coloredText = colorizeMessage(level, level.name() + " level message");
            out.println("  " + coloredText);
        }
        out.flush();
    }
}
