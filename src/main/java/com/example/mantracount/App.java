package com.example.mantracount;

import javafx.application.Application;

public class App {
    public static void main(String[] args) {
        // Maneira mais simples para compilação nativa com GraalVM
        Application.launch(MantraUI.class, args);
    }
}