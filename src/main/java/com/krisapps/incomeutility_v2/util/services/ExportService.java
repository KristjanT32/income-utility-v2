package com.krisapps.incomeutility_v2.util.services;

import com.krisapps.incomeutility_v2.types.fiscal.Transaction;

import java.nio.file.Path;
import java.util.List;

public class ExportService {
    private static ExportService instance;

    private ExportService() {

    }

    public static ExportService getInstance() {
        if (instance == null) {
            instance = new ExportService();
        }
        return instance;
    }

    public String exportAsPlainText(List<? extends Transaction> transactions) {
        StringBuilder exported = new StringBuilder();
        for (Transaction t : transactions) {
            exported.append(t.asStringRepresentation()).append("\n");
        }

        return exported.toString();
    }

    public Path exportAsCSV(List<? extends Transaction> transactions) {
        return null;
    }

    public Path exportAsExcelWorksheet(List<? extends Transaction> transactions) {
        return null;
    }
}
