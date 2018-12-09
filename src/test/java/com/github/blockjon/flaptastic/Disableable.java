package com.github.blockjon.flaptastic;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;


public class Disableable implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(
            ExtensionContext context) {
        return ConditionEvaluationResult.disabled("Disabled via flaptastic.");
//        return ConditionEvaluationResult.enabled(
//                "Test enabled on QA environment");
    }
}
