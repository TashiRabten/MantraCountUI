package com.example.mantracount;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MantraData {
    private LocalDate targetDate;
    private String nameToCount;
    private String fizKeyword;
    private String mantrasKeyword;
    private List<String> lines;



    private long totalNameCount;
    private long totalFizCount;
    private long totalMantrasCount;
    private long totalFizNumbersSum;
    private List<String> debugLines;

    // Constructor
    public MantraData() {
        this.debugLines = new ArrayList<>();
        this.fizKeyword = "fiz";          // hardcoded
        this.mantrasKeyword = "mantras";  // hardcoded
    }


    public void resetCounts() {
        this.totalNameCount = 0;
        this.totalFizCount = 0;
        this.totalMantrasCount = 0;
        this.totalFizNumbersSum = 0;
        this.debugLines.clear();
    }

    // Getters and Setters
    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getNameToCount() {
        return nameToCount;
    }

    public void setNameToCount(String nameToCount) {
        this.nameToCount = nameToCount.toLowerCase();
    }

    public String getFizKeyword() {
        return fizKeyword;
    }

    public void setFizKeyword(String fizKeyword) {
        this.fizKeyword = fizKeyword.toLowerCase();
    }

    public String getMantrasKeyword() {
        return mantrasKeyword;
    }

    public void setMantrasKeyword(String mantrasKeyword) {
        this.mantrasKeyword = mantrasKeyword.toLowerCase();
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public long getTotalNameCount() {
        return totalNameCount;
    }

    public void setTotalNameCount(long totalNameCount) {
        this.totalNameCount = totalNameCount;
    }

    public long getTotalFizCount() {
        return totalFizCount;
    }

    public void setTotalFizCount(long totalFizCount) {
        this.totalFizCount = totalFizCount;
    }

    public long getTotalMantrasCount() {
        return totalMantrasCount;
    }

    public void setTotalMantrasCount(long totalMantrasCount) {
        this.totalMantrasCount = totalMantrasCount;
    }

    public long getTotalFizNumbersSum() {
        return totalFizNumbersSum;
    }

    public void setTotalFizNumbersSum(long totalFizNumbersSum) {
        this.totalFizNumbersSum = totalFizNumbersSum;
    }

    public List<String> getDebugLines() {
        return debugLines;
    }

    public void addDebugLine(String line) {
        this.debugLines.add(line);
    }
    private String filePath;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

}
