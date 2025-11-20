package com.loanorigination.sms.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NlpParserService {

    private static final Logger log = LoggerFactory.getLogger(NlpParserService.class);

    private final Pattern amount = Pattern.compile("(?:loan|amount|\\$)\\s*[:=]?\\s*\\$?([0-9]{3,7})(?:\\.?[0-9]{0,2})?", Pattern.CASE_INSENSITIVE);
    private final Pattern termMonths = Pattern.compile("(?:term|months?)\\s*[:=]?\\s*([0-9]{1,3})\\s*months?", Pattern.CASE_INSENSITIVE);
    private final Pattern termYears = Pattern.compile("(?:term|years?)\\s*[:=]?\\s*([0-9]{1,2})\\s*years?", Pattern.CASE_INSENSITIVE);
    private final Pattern income = Pattern.compile("(?:income|salary)\\s*[:=]?\\s*\\$?([0-9]{3,7})(?:\\.?[0-9]{0,2})?", Pattern.CASE_INSENSITIVE);
    private final Pattern debt = Pattern.compile("(?:debt|liabilities?)\\s*[:=]?\\s*\\$?([0-9]{1,7})(?:\\.?[0-9]{0,2})?", Pattern.CASE_INSENSITIVE);
    private final Pattern creditScore = Pattern.compile("(?:credit\\s*score|cs)\\s*[:=]?\\s*([0-9]{3})", Pattern.CASE_INSENSITIVE);
    private final Pattern age = Pattern.compile("(?:i'?m|i\\s*am|age\\s*is|my\\s*age)\\s*[:=]?\\s*([0-9]{1,3})(?:\\s*years?\\s*old|\\s*years?)?", Pattern.CASE_INSENSITIVE);

    public Map<String, Object> parse(String text) {
        Map<String, Object> result = new HashMap<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        extractAmount(text, result);
        extractTerm(text, result);
        extractIncome(text, result);
        extractDebt(text, result);
        extractCreditScore(text, result);
        extractAge(text, result);
        log.debug("Parsed SMS into data: {}", result);
        return result;
    }

    private void extractAmount(String text, Map<String, Object> result) {
        Matcher m = amount.matcher(text);
        if (m.find()) {
            try {
                result.put("loanAmount", new BigDecimal(m.group(1)));
            } catch (Exception ignored) {}
        }
    }

    private void extractTerm(String text, Map<String, Object> result) {
        Matcher my = termYears.matcher(text);
        if (my.find()) {
            try {
                int years = Integer.parseInt(my.group(1));
                result.put("termMonths", years * 12);
                return;
            } catch (Exception ignored) {}
        }
        Matcher mm = termMonths.matcher(text);
        if (mm.find()) {
            try {
                result.put("termMonths", Integer.parseInt(mm.group(1)));
            } catch (Exception ignored) {}
        }
    }

    private void extractIncome(String text, Map<String, Object> result) {
        Matcher m = income.matcher(text);
        if (m.find()) {
            try {
                result.put("annualIncome", new BigDecimal(m.group(1)));
            } catch (Exception ignored) {}
        }
    }

    private void extractDebt(String text, Map<String, Object> result) {
        Matcher m = debt.matcher(text);
        if (m.find()) {
            try {
                result.put("existingDebt", new BigDecimal(m.group(1)));
            } catch (Exception ignored) {}
        }
    }

    private void extractCreditScore(String text, Map<String, Object> result) {
        Matcher m = creditScore.matcher(text);
        if (m.find()) {
            try {
                result.put("creditScore", Integer.parseInt(m.group(1)));
            } catch (Exception ignored) {}
        }
    }

    private void extractAge(String text, Map<String, Object> result) {
        Matcher m = age.matcher(text);
        if (m.find()) {
            try {
                result.put("applicantAge", Integer.parseInt(m.group(1)));
            } catch (Exception ignored) {}
        }
    }
}


