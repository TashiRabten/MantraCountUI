package com.example.mantracount;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TextField;

public class MantraData {
    private LocalDate targetDate;
    private String nameToCount;
    private String fizKeyword = "fiz";
    private String mantrasKeyword = "mantras";
    private List<String> lines = new ArrayList<>();

    private long totalNameCount;
    private long totalFizCount;
    private long totalMantrasCount;
    private long totalFizNumbersSum;

    private List<String> debugLines = new ArrayList<>();

    private String filePath;
    private boolean isFromZip;
    private String originalZipPath;

    private boolean hasMismatch;

    // Add this field to MantraData.java
    private LocalDate endDate;



    public MantraData() {}

    public void resetCounts() {
        this.totalNameCount = 0;
        this.totalFizCount = 0;
        this.totalMantrasCount = 0;
        this.totalFizNumbersSum = 0;
        this.debugLines.clear();
    }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getNameToCount() { return nameToCount; }
    public void setNameToCount(String nameToCount) { this.nameToCount = nameToCount.toLowerCase(); }

    public String getFizKeyword() { return fizKeyword; }
    public void setFizKeyword(String fizKeyword) { this.fizKeyword = fizKeyword.toLowerCase(); }

    public String getMantrasKeyword() { return mantrasKeyword; }
    public void setMantrasKeyword(String mantrasKeyword) { this.mantrasKeyword = mantrasKeyword.toLowerCase(); }

    public List<String> getLines() { return lines; }
    public void setLines(List<String> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }

    public long getTotalNameCount() { return totalNameCount; }
    public void setTotalNameCount(long totalNameCount) { this.totalNameCount = totalNameCount; }

    public long getTotalFizCount() { return totalFizCount; }
    public void setTotalFizCount(long totalFizCount) { this.totalFizCount = totalFizCount; }

    public long getTotalMantrasCount() { return totalMantrasCount; }
    public void setTotalMantrasCount(long totalMantrasCount) { this.totalMantrasCount = totalMantrasCount; }

    public long getTotalFizNumbersSum() { return totalFizNumbersSum; }
    public void setTotalFizNumbersSum(long totalFizNumbersSum) { this.totalFizNumbersSum = totalFizNumbersSum; }

    public List<String> getDebugLines() { return debugLines; }
    public void addDebugLine(String line) { this.debugLines.add(line); }

    public void setDebugLines(List<String> lines) {
        this.debugLines.addAll(lines);
    }


    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public boolean isFromZip() { return isFromZip; }
    public void setFromZip(boolean fromZip) { this.isFromZip = fromZip; }

    public String getOriginalZipPath() { return originalZipPath; }
    public void setOriginalZipPath(String originalZipPath) { this.originalZipPath = originalZipPath; }

    public void setHasMismatch(boolean mismatch) {
        this.hasMismatch = mismatch;
    }
    public boolean hasMismatch() {
        return hasMismatch;
    }


    public void analyzeMismatch(int fizCount, int mantraWordsCount, int mantraKeywordCount, String line) {
        boolean mismatch = fizCount != mantraWordsCount ||
                mantraWordsCount != mantraKeywordCount ||
                LineAnalyzer.hasApproximateButNotExactMatch(line, nameToCount);

        setHasMismatch(mismatch);  // Set the mismatch flag
    }
}